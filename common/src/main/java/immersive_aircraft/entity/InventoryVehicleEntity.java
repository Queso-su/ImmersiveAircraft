package immersive_aircraft.entity;

import immersive_aircraft.WeaponRegistry;
import immersive_aircraft.cobalt.network.NetworkHandler;
import immersive_aircraft.config.Config;
import immersive_aircraft.entity.inventory.SparseSimpleInventory;
import immersive_aircraft.entity.inventory.VehicleInventoryDescription;
import immersive_aircraft.entity.inventory.slots.SlotDescription;
import immersive_aircraft.entity.misc.VehicleProperties;
import immersive_aircraft.entity.misc.WeaponMount;
import immersive_aircraft.entity.weapon.Telescope;
import immersive_aircraft.entity.weapon.Weapon;
import immersive_aircraft.item.WeaponItem;
import immersive_aircraft.item.upgrade.VehicleStat;
import immersive_aircraft.item.upgrade.VehicleUpgrade;
import immersive_aircraft.item.upgrade.VehicleUpgradeRegistry;
import immersive_aircraft.mixin.ServerPlayerEntityMixin;
import immersive_aircraft.network.s2c.OpenGuiRequest;
import immersive_aircraft.screen.VehicleScreenHandler;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;

import java.util.*;

public abstract class InventoryVehicleEntity extends DyeableVehicleEntity implements ContainerListener, MenuProvider, Container, HasCustomInventoryScreen {
    private final VehicleProperties properties;
    private SparseSimpleInventory inventory;
    protected final Map<Integer, List<Weapon>> weapons = new HashMap<>();

    public InventoryVehicleEntity(EntityType<? extends InventoryVehicleEntity> entityType, Level world, boolean canExplodeOnCrash) {
        super(entityType, world, canExplodeOnCrash);

        this.initInventory();

        this.properties = new VehicleProperties(getVehicleData().getProperties(), this);
    }

    public VehicleProperties getProperties() {
        return properties;
    }

    public VehicleInventoryDescription getInventoryDescription() {
        return getVehicleData().getInventoryDescription();
    }

    private static final List<WeaponMount> EMPTY_WEAPONS = List.of(WeaponMount.EMPTY);
    private static final Map<WeaponMount.Type, List<WeaponMount>> EMPTY_WEAPONS_MAP = Map.of();

    public List<WeaponMount> getWeaponMounts(int slot) {
        ItemStack stack = getSlot(slot).get();
        if (stack.getItem() instanceof WeaponItem weaponItem) {
            return getVehicleData().getWeaponMounts().getOrDefault(slot, EMPTY_WEAPONS_MAP).getOrDefault(weaponItem.getMountType(), EMPTY_WEAPONS);
        }
        return EMPTY_WEAPONS;
    }

    public List<ItemStack> getSlots(String slotType) {
        List<SlotDescription> slots = getInventoryDescription().getSlots(slotType);
        List<ItemStack> list = new ArrayList<>(slots.size());
        for (SlotDescription slot : slots) {
            list.add(getInventory().getItem(slot.index()));
        }
        return list;
    }

    //todo cache?
    public float getTotalUpgrade(VehicleStat stat) {
        float value = 1.0f;
        List<ItemStack> upgrades = getSlots(VehicleInventoryDescription.UPGRADE);
        for (int step = 0; step < 2; step++) {
            for (ItemStack stack : upgrades) {
                VehicleUpgrade upgrade = VehicleUpgradeRegistry.INSTANCE.getUpgrade(stack.getItem());
                if (upgrade != null) {
                    float u = upgrade.get(stat);

                    if (u > 0 && step == 1)
                        value += u;
                    else if (u < 0 && step == 0)
                        value *= (u + 1);
                }
            }
        }
        return Math.max(0.0f, value);
    }

    protected void initInventory() {
        this.inventory = new SparseSimpleInventory(getInventoryDescription().getInventorySize());
        this.inventory.addListener(this);
    }

    public SparseSimpleInventory getInventory() {
        int inventorySize = getInventoryDescription().getInventorySize();
        if (inventorySize != inventory.getContainerSize()) {
            // Save current items
            NonNullList<ItemStack> items = inventory.getItems();
            initInventory();
            // Restore items to the new inventory
            for (int i = 0; i < Math.min(items.size(), inventory.getContainerSize()); i++) {
                inventory.setItem(i, items.get(i));
            }
        }
        return inventory;
    }

    @Override
    public void containerChanged(@NonNull Container sender) {

    }

    @Override
    protected void dropInventory(ServerLevel serverLevel) {
        for (SlotDescription slot : getInventoryDescription().getSlots()) {
            boolean isCargo = slot.type().equals(VehicleInventoryDescription.INVENTORY);
            if (isCargo && Config.getInstance().dropInventory || !isCargo && Config.getInstance().dropUpgrades) {
                ItemStack stack = getSlot(slot.index()).get();
                if (!stack.isEmpty()) {
                    this.spawnAtLocation(serverLevel, stack.copyAndClear());
                }
            }
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player playerEntity) {
        return new VehicleScreenHandler(i, playerInventory, this);
    }

    public void openInventory(ServerPlayer player) {
        ((ServerPlayerEntityMixin)player).ic$nextContainerCounter();
        AbstractContainerMenu screenHandler = createMenu(((ServerPlayerEntityMixin)player).getContainerCounter(), player.getInventory(), player);
        if (screenHandler != null) {
            NetworkHandler.sendToPlayer(new OpenGuiRequest(this, screenHandler.containerId), player);
            player.containerMenu = screenHandler;
            ServerPlayerEntityMixin playerAccessor = (ServerPlayerEntityMixin) player;
            screenHandler.setSynchronizer(playerAccessor.getContainerSynchronizer());
        }
    }

    @Override
    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand) {
        if (getHealth() >= 1.0) {
            if (!player.level().isClientSide() && player.isSecondaryUseActive() && !isPassengerOfSameVehicle(player)) {
                Entity primaryPassenger = getFirstPassenger();
                if (primaryPassenger != null) {
                    // Kick out the first passenger
                    primaryPassenger.stopRiding();
                } else {
                    // Open inventory instead
                    openInventory((ServerPlayer) player);
                }
                return InteractionResult.CONSUME;
            } else if (getPassengerSpace() == 0 && player instanceof ServerPlayer serverPlayer) {
                // For vehicles without passengers, just open inventory
                openInventory(serverPlayer);
            }
        }
        return super.interact(player, hand);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        
        // Save items by slot type, preserving order and only saving non-empty slots
        // Boiler items (fuel)
        ValueOutput.TypedOutputList<ItemStack> boilerItems = tag.list("BoilerItems", ItemStack.CODEC);
        for (SlotDescription slot : getInventoryDescription().getSlots(VehicleInventoryDescription.BOILER)) {
            ItemStack itemStack = getInventory().getItem(slot.index());
            if (!itemStack.isEmpty()) {
                boilerItems.add(itemStack);
            }
        }
        
        // Booster items (rockets)
        ValueOutput.TypedOutputList<ItemStack> boosterItems = tag.list("BoosterItems", ItemStack.CODEC);
        for (SlotDescription slot : getInventoryDescription().getSlots(VehicleInventoryDescription.BOOSTER)) {
            ItemStack itemStack = getInventory().getItem(slot.index());
            if (!itemStack.isEmpty()) {
                boosterItems.add(itemStack);
            }
        }
        
        // Weapon items
        ValueOutput.TypedOutputList<ItemStack> weaponItems = tag.list("WeaponItems", ItemStack.CODEC);
        for (SlotDescription slot : getInventoryDescription().getSlots(VehicleInventoryDescription.WEAPON)) {
            ItemStack itemStack = getInventory().getItem(slot.index());
            if (!itemStack.isEmpty()) {
                weaponItems.add(itemStack);
            }
        }
        
        // Upgrade items
        ValueOutput.TypedOutputList<ItemStack> upgradeItems = tag.list("UpgradeItems", ItemStack.CODEC);
        for (SlotDescription slot : getInventoryDescription().getSlots(VehicleInventoryDescription.UPGRADE)) {
            ItemStack itemStack = getInventory().getItem(slot.index());
            if (!itemStack.isEmpty()) {
                upgradeItems.add(itemStack);
            }
        }
        
        // Banner items
        ValueOutput.TypedOutputList<ItemStack> bannerItems = tag.list("BannerItems", ItemStack.CODEC);
        for (SlotDescription slot : getInventoryDescription().getSlots(VehicleInventoryDescription.BANNER)) {
            ItemStack itemStack = getInventory().getItem(slot.index());
            if (!itemStack.isEmpty()) {
                bannerItems.add(itemStack);
            }
        }
        
        // Dye items
        ValueOutput.TypedOutputList<ItemStack> dyeItems = tag.list("DyeItems", ItemStack.CODEC);
        for (SlotDescription slot : getInventoryDescription().getSlots(VehicleInventoryDescription.DYE)) {
            ItemStack itemStack = getInventory().getItem(slot.index());
            if (!itemStack.isEmpty()) {
                dyeItems.add(itemStack);
            }
        }
        
        // Inventory items (cargo)
        ValueOutput.TypedOutputList<ItemStack> inventoryItems = tag.list("InventoryItems", ItemStack.CODEC);
        for (SlotDescription slot : getInventoryDescription().getSlots(VehicleInventoryDescription.INVENTORY)) {
            ItemStack itemStack = getInventory().getItem(slot.index());
            if (!itemStack.isEmpty()) {
                inventoryItems.add(itemStack);
            }
        }
        
        // For backward compatibility
        ValueOutput.TypedOutputList<ItemStack> list = tag.list("Inventory", ItemStack.CODEC);
        getInventory().storeAsItemList(list);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull ValueInput tag) {
        super.readAdditionalSaveData(tag);
        
        // Get inventory and ensure it's initialized with correct size
        SparseSimpleInventory inventory = getInventory();
        
        // Clear inventory
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            inventory.setItem(i, ItemStack.EMPTY);
        }
        
        // Try to load items by slot type (new format), preserving order and handling empty slots
        boolean loadedByType = false;
        
        // Load boiler items (fuel)
        ValueInput.TypedInputList<ItemStack> boilerItems = tag.listOrEmpty("BoilerItems", ItemStack.CODEC);
        List<SlotDescription> boilerSlots = getInventoryDescription().getSlots(VehicleInventoryDescription.BOILER);
        if (!boilerSlots.isEmpty()) {
            loadedByType = true;
            int itemIndex = 0;
            for (ItemStack itemStack : boilerItems) {
                if (itemIndex < boilerSlots.size()) {
                    inventory.setItem(boilerSlots.get(itemIndex).index(), itemStack);
                    itemIndex++;
                } else {
                    break;
                }
            }
        }
        
        // Load booster items (rockets)
        ValueInput.TypedInputList<ItemStack> boosterItems = tag.listOrEmpty("BoosterItems", ItemStack.CODEC);
        List<SlotDescription> boosterSlots = getInventoryDescription().getSlots(VehicleInventoryDescription.BOOSTER);
        if (!boosterSlots.isEmpty()) {
            loadedByType = true;
            int itemIndex = 0;
            for (ItemStack itemStack : boosterItems) {
                if (itemIndex < boosterSlots.size()) {
                    inventory.setItem(boosterSlots.get(itemIndex).index(), itemStack);
                    itemIndex++;
                } else {
                    break;
                }
            }
        }
        
        // Load weapon items
        ValueInput.TypedInputList<ItemStack> weaponItems = tag.listOrEmpty("WeaponItems", ItemStack.CODEC);
        List<SlotDescription> weaponSlots = getInventoryDescription().getSlots(VehicleInventoryDescription.WEAPON);
        if (!weaponSlots.isEmpty()) {
            loadedByType = true;
            int itemIndex = 0;
            for (ItemStack itemStack : weaponItems) {
                if (itemIndex < weaponSlots.size()) {
                    inventory.setItem(weaponSlots.get(itemIndex).index(), itemStack);
                    itemIndex++;
                } else {
                    break;
                }
            }
        }
        
        // Load upgrade items
        ValueInput.TypedInputList<ItemStack> upgradeItems = tag.listOrEmpty("UpgradeItems", ItemStack.CODEC);
        List<SlotDescription> upgradeSlots = getInventoryDescription().getSlots(VehicleInventoryDescription.UPGRADE);
        if (!upgradeSlots.isEmpty()) {
            loadedByType = true;
            int itemIndex = 0;
            for (ItemStack itemStack : upgradeItems) {
                if (itemIndex < upgradeSlots.size()) {
                    inventory.setItem(upgradeSlots.get(itemIndex).index(), itemStack);
                    itemIndex++;
                } else {
                    break;
                }
            }
        }
        
        // Load banner items
        ValueInput.TypedInputList<ItemStack> bannerItems = tag.listOrEmpty("BannerItems", ItemStack.CODEC);
        List<SlotDescription> bannerSlots = getInventoryDescription().getSlots(VehicleInventoryDescription.BANNER);
        if (!bannerSlots.isEmpty()) {
            loadedByType = true;
            int itemIndex = 0;
            for (ItemStack itemStack : bannerItems) {
                if (itemIndex < bannerSlots.size()) {
                    inventory.setItem(bannerSlots.get(itemIndex).index(), itemStack);
                    itemIndex++;
                } else {
                    break;
                }
            }
        }
        
        // Load dye items
        ValueInput.TypedInputList<ItemStack> dyeItems = tag.listOrEmpty("DyeItems", ItemStack.CODEC);
        List<SlotDescription> dyeSlots = getInventoryDescription().getSlots(VehicleInventoryDescription.DYE);
        if (!dyeSlots.isEmpty()) {
            loadedByType = true;
            int itemIndex = 0;
            for (ItemStack itemStack : dyeItems) {
                if (itemIndex < dyeSlots.size()) {
                    inventory.setItem(dyeSlots.get(itemIndex).index(), itemStack);
                    itemIndex++;
                } else {
                    break;
                }
            }
        }
        
        // Load inventory items (cargo)
        ValueInput.TypedInputList<ItemStack> inventoryItems = tag.listOrEmpty("InventoryItems", ItemStack.CODEC);
        List<SlotDescription> inventorySlots = getInventoryDescription().getSlots(VehicleInventoryDescription.INVENTORY);
        if (!inventorySlots.isEmpty()) {
            loadedByType = true;
            int itemIndex = 0;
            for (ItemStack itemStack : inventoryItems) {
                if (itemIndex < inventorySlots.size()) {
                    inventory.setItem(inventorySlots.get(itemIndex).index(), itemStack);
                    itemIndex++;
                } else {
                    break;
                }
            }
        }
        
        // If not loaded by type, use old format for backward compatibility
        if (!loadedByType) {
            ValueInput.TypedInputList<ItemStack> list = tag.listOrEmpty("Inventory", ItemStack.CODEC);
            
            // Get slot descriptions by type
            Map<String, List<SlotDescription>> slotsByType = new HashMap<>();
            for (SlotDescription slot : getInventoryDescription().getSlots()) {
                slotsByType.computeIfAbsent(slot.type(), k -> new ArrayList<>()).add(slot);
            }
            
            // Separate items by type
            Map<String, List<ItemStack>> itemsByType = new HashMap<>();
            for (ItemStack itemStack : list) {
                if (itemsByType.size() >= inventory.getContainerSize()) {
                    break;
                }
                
                // Determine item type
                String itemType = "inventory"; // Default to inventory
                if (!itemStack.isEmpty()) {
                    // Check which slot type can accept this item
                    for (Map.Entry<String, List<SlotDescription>> entry : slotsByType.entrySet()) {
                        String slotType = entry.getKey();
                        List<SlotDescription> slots = entry.getValue();
                        if (!slots.isEmpty()) {
                            SlotDescription slot = slots.get(0);
                            Slot slotInstance = slot.getSlot(this, inventory);
                            if (slotInstance.mayPlace(itemStack)) {
                                itemType = slotType;
                                break;
                            }
                        }
                    }
                }
                
                itemsByType.computeIfAbsent(itemType, k -> new ArrayList<>()).add(itemStack);
            }
            
            // Fill items into slots by type
            for (Map.Entry<String, List<ItemStack>> entry : itemsByType.entrySet()) {
                String itemType = entry.getKey();
                List<ItemStack> items = entry.getValue();
                List<SlotDescription> slots = slotsByType.get(itemType);
                if (slots != null) {
                    int itemIndex = 0;
                    for (ItemStack itemStack : items) {
                        if (itemIndex < slots.size()) {
                            inventory.setItem(slots.get(itemIndex).index(), itemStack);
                            itemIndex++;
                        } else {
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void addItemTag(ItemStack stack) {
        super.addItemTag(stack);

        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(getInventory().getItems()));
    }

    @Override
    public void readItemTag(ItemStack stack) {
        super.readItemTag(stack);

        // Ensure inventory is initialized with correct size before loading items
        getInventory();
        
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents != null) {
            contents.copyInto(getInventory().getItems());
        }
    }

    @Override
    public void boost() {
        int length = getSlots(VehicleInventoryDescription.BOOSTER).stream().mapToInt(s -> {
            Fireworks fireworks = s.get(DataComponents.FIREWORKS);
            int l = fireworks == null ? 1 : fireworks.flightDuration();
            s.shrink(1);
            return l;
        }).sum();

        super.boost(length * 50);
    }

    @Override
    protected void applyBoost() {
        super.applyBoost();

        // boost
        Vector3f direction = getForwardDirection();
        float thrust = 0.05f * getBoost() / 100.0f;
        setDeltaMovement(getDeltaMovement().add(toVec3d(direction.mul(thrust))));

        // particles
        if (tickCount % 2 == 0) {
            Vec3 p = position();
            Vec3 velocity = getDeltaMovement().subtract(toVec3d(direction));
            level().addParticle(ParticleTypes.FIREWORK, p.x(), p.y(), p.z(), velocity.x, velocity.y, velocity.z);
        }
    }

    @Override
    public boolean canBoost() {
        return getSlots(VehicleInventoryDescription.BOOSTER).stream().anyMatch(v -> !v.isEmpty()) && getBoost() <= 0;
    }

    @Override
    public void tick() {
        getInventory().tick(this);

        // Check and recreate weapon slots
        for (SlotDescription slot : getInventoryDescription().getSlots(VehicleInventoryDescription.WEAPON)) {
            ItemStack weaponItemStack = getSlot(slot.index()).get();
            List<Weapon> weapon = weapons.get(slot.index());

            if (weaponItemStack.isEmpty() && weapon != null) {
                weapons.remove(slot.index());
            } else if (!weaponItemStack.isEmpty() && (weapon == null || weapon.get(0).getStack() != weaponItemStack)) {
                WeaponRegistry.WeaponConstructor constructor = WeaponRegistry.get(weaponItemStack);
                if (constructor != null) {
                    List<WeaponMount> weaponMounts = getWeaponMounts(slot.index());
                    ArrayList<Weapon> weapons = new ArrayList<>(weaponMounts.size());
                    for (WeaponMount weaponMount : weaponMounts) {
                        weapons.add(constructor.create(this, weaponItemStack, weaponMount, slot.index()));
                    }
                    this.weapons.put(slot.index(), weapons);
                }
            }
        }

        // Update gunner offsets
        // The first weapon is assigned to the last passenger, the second to the second last, etc.
        // If more weapons than passengers are available, the remaining weapons are assigned to the driver
        int gunnerOffset = getPassengers().size();
        for (List<Weapon> weapons : getWeapons().values()) {
            gunnerOffset--;
            for (Weapon weapon : weapons) {
                weapon.setGunnerOffset(Math.max(0, gunnerOffset));
            }
        }

        // Update weapons
        for (List<Weapon> weapons : weapons.values()) {
            for (Weapon w : weapons) {
                w.tick();
            }
        }

        super.tick();
    }

    protected float getGroundDecay() {
        return getProperties().get(VehicleStat.GROUND_FRICTION);
    }

    protected float getWaterDecay() {
        return getProperties().get(VehicleStat.WATER_FRICTION);
    }

    protected void applyFriction() {
        // Decay is the basic factor of friction, basically the density of the material slowing down the vehicle
        float decay = 1.0f - getProperties().get(VehicleStat.FRICTION);
        double gravity = getGravity();
        if (wasTouchingWater) {
            gravity *= 0.25f;
            decay = getWaterDecay();
        } else if (onGround()) {
            if (isVehicle()) {
                decay = getGroundDecay();
            } else {
                decay = 0.75f;
            }
        }

        // Velocity decay
        Vec3 velocity = getDeltaMovement();
        float hd = getProperties().get(VehicleStat.HORIZONTAL_DECAY);
        float vd = getProperties().get(VehicleStat.VERTICAL_DECAY);
        setDeltaMovement(velocity.x * decay * hd, velocity.y * decay * vd - gravity, velocity.z * decay * hd);

        // Rotation decay
        float rf = decay * getProperties().get(VehicleStat.ROTATION_DECAY);
        pressingInterpolatedX.decay(0.0f, 1.0f - rf);
        pressingInterpolatedZ.decay(0.0f, 1.0f - rf);
    }

    @Override
    public SlotAccess getSlot(int slot) {

        return SlotAccess.of(() -> getInventory().getItem(slot), stack -> getInventory().setItem(slot, stack));
    }

    public Map<Integer, List<Weapon>> getWeapons() {
        return weapons;
    }

    @Override
    public float getDurability() {
        return getProperties().get(VehicleStat.DURABILITY);
    }

    public boolean isScoping() {
        Collection<List<Weapon>> values = getWeapons().values();
        for (List<Weapon> weapons : values) {
            for (Weapon weapon : weapons) {
                if (weapon instanceof Telescope telescope && telescope.isScoping()) {
                    return true;
                }
            }
        }
        return false;
    }

    // Inventory proxy methods

    @Override
    public int getContainerSize() {
        return inventory.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return inventory.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return inventory.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return inventory.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return inventory.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory.setItem(slot, stack);
    }

    @Override
    public void setChanged() {
        inventory.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return inventory.stillValid(player);
    }

    @Override
    public void clearContent() {
        inventory.clearContent();
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        SlotDescription slotType = getInventoryDescription().getSlots().get(index);
        Slot slot = slotType.getSlot(this, inventory);
        return slot.mayPlace(stack);
    }

    @Override
    public boolean canTakeItem(Container target, int index, ItemStack stack) {
        SlotDescription slotType = getInventoryDescription().getSlots().get(index);
        return slotType.type().equals(VehicleInventoryDescription.INVENTORY);
    }

    public void clientFireWeapons(Entity entity) {
        int gunnerIndex = getPassengers().indexOf(entity);
        for (List<Weapon> weapons : getWeapons().values()) {
            int index = 0;
            for (Weapon weapon : weapons) {
                if (weapon.getGunnerOffset() == gunnerIndex) {
                    weapon.clientFire(index++);
                }
            }
        }
    }

    public void fireWeapon(int slot, int index, Vector3f direction) {
        getWeapons().get(slot).get(index).fire(direction);
    }

    @Override
    public void openCustomInventoryScreen(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            openInventory(serverPlayer);
        }
    }
}