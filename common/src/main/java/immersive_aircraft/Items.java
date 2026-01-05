package immersive_aircraft;

import immersive_aircraft.cobalt.registration.Registration;
import immersive_aircraft.entity.*;
import immersive_aircraft.entity.misc.WeaponMount;
import immersive_aircraft.item.AircraftItem;
import immersive_aircraft.item.DyeableAircraftItem;
import immersive_aircraft.item.WeaponItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class Items {
    private static final List<Supplier<Item>> items = new LinkedList<>();

    public static Supplier<Item> HULL;
    public static Supplier<Item> ENGINE;
    public static Supplier<Item> SAIL;
    public static Supplier<Item> PROPELLER;
    public static Supplier<Item> BOILER;

    public static Supplier<Item> AIRSHIP;
    public static Supplier<Item> CARGO_AIRSHIP;
    public static Supplier<Item> WARSHIP;
    public static Supplier<Item> BIPLANE;
    public static Supplier<Item> GYRODYNE;
    public static Supplier<Item> QUADROCOPTER;
    public static Supplier<Item> BAMBOO_HOPPER;

    public static Supplier<Item> ROTARY_CANNON;
    public static Supplier<Item> HEAVY_CROSSBOW;
    public static Supplier<Item> TELESCOPE;
    public static Supplier<Item> BOMB_BAY;

    public static Supplier<Item> ENHANCED_PROPELLER;
    public static Supplier<Item> ECO_ENGINE;
    public static Supplier<Item> NETHER_ENGINE;
    public static Supplier<Item> STEEL_BOILER;
    public static Supplier<Item> INDUSTRIAL_GEARS;
    public static Supplier<Item> STURDY_PIPES;
    public static Supplier<Item> GYROSCOPE;
    public static Supplier<Item> GYROSCOPE_HUD;
    public static Supplier<Item> GYROSCOPE_DIALS;
    public static Supplier<Item> HULL_REINFORCEMENT;
    public static Supplier<Item> IMPROVED_LANDING_GEAR;

    private static Supplier<Item> register(String name, Function<Item.Properties, Item> itemFactory, Item.Properties properties) {
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Main.MOD_ID, name));
        Item.Properties itemProperties = properties.setId(itemKey);
        Supplier<Item> register = Registration.register(BuiltInRegistries.ITEM, Main.locate(name), () -> itemFactory.apply(itemProperties));
        items.add(register);
        return register;
    }

    public static void bootstrap() {
        HULL = register("hull", Item::new, baseProps().stacksTo(8));
        ENGINE = register("engine", Item::new, baseProps().stacksTo(8));
        SAIL = register("sail", Item::new, baseProps().stacksTo(8));
        PROPELLER = register("propeller", Item::new, baseProps().stacksTo(8));
        BOILER = register("boiler", Item::new, baseProps().stacksTo(8));

        AIRSHIP = register("airship",
                (props) -> new DyeableAircraftItem(props, world -> new AirshipEntity(Entities.AIRSHIP.get(), world)),
                baseProps().stacksTo(1));
        CARGO_AIRSHIP = register("cargo_airship",
                (props) -> new DyeableAircraftItem(props, world -> new CargoAirshipEntity(Entities.CARGO_AIRSHIP.get(), world)),
                baseProps().stacksTo(1));
        WARSHIP = register("warship",
                (props) -> new DyeableAircraftItem(props, world -> new WarshipEntity(Entities.WARSHIP.get(), world)),
                baseProps().stacksTo(1));
        BIPLANE = register("biplane",
                (props) -> new AircraftItem(props, world -> new BiplaneEntity(Entities.BIPLANE.get(), world)),
                baseProps().stacksTo(1));
        GYRODYNE = register("gyrodyne",
                (props) -> new AircraftItem(props, world -> new GyrodyneEntity(Entities.GYRODYNE.get(), world)),
                baseProps().stacksTo(1)
        );
        QUADROCOPTER = register("quadrocopter",
                (props) -> new AircraftItem(props, world -> new QuadrocopterEntity(Entities.QUADROCOPTER.get(), world)),
                baseProps().stacksTo(1));
        BAMBOO_HOPPER = register("bamboo_hopper",
                (props) -> new AircraftItem(props, world -> new BambooHopperEntity(Entities.BAMBOO_HOPPER.get(), world)),
                baseProps().stacksTo(1));

        ROTARY_CANNON = register("rotary_cannon",
                (props) -> new WeaponItem(props, WeaponMount.Type.ROTATING), baseProps().stacksTo(1));
        HEAVY_CROSSBOW = register("heavy_crossbow",
                (props) -> new WeaponItem(props, WeaponMount.Type.FRONT), baseProps().stacksTo(1));
        TELESCOPE = register("telescope",
                (props) -> new WeaponItem(props, WeaponMount.Type.ROTATING), baseProps().stacksTo(1));
        BOMB_BAY = register("bomb_bay",
                (props) -> new WeaponItem(props, WeaponMount.Type.DROP), baseProps().stacksTo(1));

        ENHANCED_PROPELLER = register("enhanced_propeller",
                Item::new, baseProps().stacksTo(8));
        ECO_ENGINE = register("eco_engine",
                Item::new, baseProps().stacksTo(8));
        NETHER_ENGINE = register("nether_engine",
                Item::new, baseProps().stacksTo(8));
        STEEL_BOILER = register("steel_boiler",
                Item::new, baseProps().stacksTo(8));
        INDUSTRIAL_GEARS = register("industrial_gears",
                Item::new, baseProps().stacksTo(8));
        STURDY_PIPES = register("sturdy_pipes",
                Item::new, baseProps().stacksTo(8));
        GYROSCOPE = register("gyroscope",
                Item::new, baseProps().stacksTo(8));
        GYROSCOPE_HUD = register("gyroscope_hud",
                Item::new, baseProps().stacksTo(8));
        GYROSCOPE_DIALS = register("gyroscope_dials",
                Item::new, baseProps().stacksTo(8));
        HULL_REINFORCEMENT = register("hull_reinforcement",
                Item::new, baseProps().stacksTo(8));
        IMPROVED_LANDING_GEAR = register("improved_landing_gear",
                Item::new, baseProps().stacksTo(8));
    }

    private static Item.Properties baseProps() {
        return new Item.Properties();
    }

    public static List<ItemStack> getSortedItems() {
        return items.stream().map(i -> i.get().getDefaultInstance()).toList();
    }
}