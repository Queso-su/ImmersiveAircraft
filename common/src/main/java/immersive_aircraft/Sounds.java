package immersive_aircraft;

import immersive_aircraft.cobalt.registration.Registration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

public class Sounds {
    public static Supplier<SoundEvent> ENGINE_START;
    public static Supplier<SoundEvent> ENGINE_START_BAMBOO_HOPPER;
    public static Supplier<SoundEvent> ENGINE_START_WARSHIP;
    public static Supplier<SoundEvent> WARSHIP;
    public static Supplier<SoundEvent> PROPELLER;
    public static Supplier<SoundEvent> PROPELLER_BAMBOO_HOPPER;
    public static Supplier<SoundEvent> PROPELLER_SMALL;
    public static Supplier<SoundEvent> PROPELLER_TINY;
    public static Supplier<SoundEvent> WOOSH;
    public static Supplier<SoundEvent> REPAIR;
    public static Supplier<SoundEvent> CANNON;

    public static void bootstrap() {
        ENGINE_START = register("engine_start");
        ENGINE_START_BAMBOO_HOPPER = register("engine_start_bamboo_hopper");
        ENGINE_START_WARSHIP = register("engine_start_warship");
        WARSHIP = register("warship");
        PROPELLER = register("propeller");
        PROPELLER_BAMBOO_HOPPER = register("propeller_bamboo_hopper");
        PROPELLER_SMALL = register("propeller_small");
        PROPELLER_TINY = register("propeller_tiny");
        WOOSH = register("woosh");
        REPAIR = register("repair");
        CANNON = register("cannon");
    }

    private static Supplier<SoundEvent> register(String name) {
        Identifier id = Main.locate(name);
        return Registration.register(BuiltInRegistries.SOUND_EVENT, id, () -> SoundEvent.createVariableRangeEvent(id));
    }
}