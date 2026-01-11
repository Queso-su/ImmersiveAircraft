package immersive_aircraft.resources.bbmodel;

import org.mariuszgromada.math.mxparser.Argument;

import java.util.HashMap;
import java.util.Map;

public class BBAnimationVariables {
    // 使用静态注册表，与1.21版本一致
    public static final Map<String, Argument> REGISTRY = new HashMap<>();

    static {
        // 注册所有动画变量
        AnimationVariableName.getAllNames().forEach(BBAnimationVariables::register);
    }

    private static void register(String name) {
        REGISTRY.put(name, new Argument("variable_" + name, 0));
    }

    public static Argument[] getArgumentArray() {
        return REGISTRY.values().toArray(new Argument[0]);
    }

    public BBAnimationVariables() {
        // 保持实例化构造器，确保向后兼容
    }



    public void set(AnimationVariableName name, float value) {
        REGISTRY.get(name.getName()).setArgumentValue(value);
    }
}
