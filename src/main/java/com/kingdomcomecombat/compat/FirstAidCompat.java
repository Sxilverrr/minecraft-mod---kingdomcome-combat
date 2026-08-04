package com.kingdomcomecombat.compat;

import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.collision.HumanoidHurtboxLibrary;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/** Soft bridge that lets First Aid distribute KCC damage to the hurtbox KCC actually hit. */
public final class FirstAidCompat {
    private static final ThreadLocal<TargetPart> ACTIVE_PART = new ThreadLocal<>();
    private static volatile boolean reflectionFailureLogged;

    private FirstAidCompat() {
    }

    public static Scope target(LivingEntity entity, HumanoidHurtboxLibrary.Part part, String detailedPart) {
        return target(entity, part, detailedPart, 1.0F);
    }

    public static Scope target(
            LivingEntity entity,
            HumanoidHurtboxLibrary.Part part,
            String detailedPart,
            float damageScale
    ) {
        if (!(entity instanceof PlayerEntity)) {
            return Scope.NOOP;
        }
        TargetPart previous = ACTIVE_PART.get();
        ACTIVE_PART.set(new TargetPart(
                firstAidPart(part, detailedPart),
                Math.max(0.0F, Float.isFinite(damageScale) ? damageScale : 1.0F)
        ));
        return () -> {
            if (previous == null) {
                ACTIVE_PART.remove();
            } else {
                ACTIVE_PART.set(previous);
            }
        };
    }

    /** Called by the optional pseudo-mixin without linking KCC against First Aid at compile time. */
    public static Object forcedDistribution() {
        TargetPart target = ACTIVE_PART.get();
        if (target == null) {
            return null;
        }
        try {
            ClassLoader loader = FirstAidCompat.class.getClassLoader();
            Class<?> algorithm = Class.forName(
                    "ichttt.mods.firstaid.api.distribution.IDamageDistributionAlgorithm", false, loader);
            return Proxy.newProxyInstance(loader, new Class<?>[]{algorithm}, (proxy, method, args) -> {
                return switch (method.getName()) {
                    case "distributeDamage" -> damagePart(target.partName(), target.damageScale(), args);
                    case "skipGlobalPotionModifiers" -> false;
                    case "codec" -> null;
                    case "toString" -> "KCC First Aid distribution[" + target.partName() + "]";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.toString());
                };
            });
        } catch (ReflectiveOperationException | LinkageError exception) {
            logReflectionFailure(exception);
            return null;
        }
    }

    public static String activePartName() {
        TargetPart target = ACTIVE_PART.get();
        return target == null ? null : target.partName();
    }

    public static float activeDamageScale() {
        TargetPart target = ACTIVE_PART.get();
        return target == null ? 1.0F : target.damageScale();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static float damagePart(String partName, float damageScale, Object[] args) throws ReflectiveOperationException {
        float originalAmount = ((Number) args[0]).floatValue();
        float amount = originalAmount * damageScale;
        Object player = args[1];
        boolean applyDebuff = (Boolean) args[3];

        Class<?> holderClass = Class.forName("ichttt.mods.firstaid.common.FirstAidDamageModelHolder");
        if (!holderClass.isInstance(player)) {
            return originalAmount;
        }
        Object model = holderClass.getMethod("firstaid$getDamageModel").invoke(player);
        Class<? extends Enum> enumClass = (Class<? extends Enum>) Class.forName(
                "ichttt.mods.firstaid.api.enums.EnumPlayerPart").asSubclass(Enum.class);
        Object partEnum = Enum.valueOf(enumClass, partName);
        Method getFromEnum = model.getClass().getMethod("getFromEnum", enumClass);
        Object damageablePart = getFromEnum.invoke(model, partEnum);

        Method damage = null;
        for (Method candidate : damageablePart.getClass().getMethods()) {
            if (candidate.getName().equals("damage") && candidate.getParameterCount() == 3) {
                damage = candidate;
                break;
            }
        }
        if (damage == null) {
            throw new NoSuchMethodException("First Aid damage(float, Player, boolean)");
        }
        float scaledLeftover = ((Number) damage.invoke(damageablePart, amount, player, applyDebuff)).floatValue();
        return damageScale <= 0.000001F ? 0.0F : scaledLeftover / damageScale;
    }

    private static String firstAidPart(HumanoidHurtboxLibrary.Part part, String detailedPart) {
        return switch (detailedPart == null ? "" : detailedPart) {
            case "face", "neck", "crown", "side_head" -> "HEAD";
            case "foot" -> isRight(part) ? "RIGHT_FOOT" : "LEFT_FOOT";
            case "thigh", "knee", "calf" -> isRight(part) ? "RIGHT_LEG" : "LEFT_LEG";
            case "shoulder", "arm", "hand" -> isRight(part) ? "RIGHT_ARM" : "LEFT_ARM";
            default -> switch (part) {
                case HEAD -> "HEAD";
                case LEFT_ARM -> "LEFT_ARM";
                case RIGHT_ARM -> "RIGHT_ARM";
                case LEFT_LEG -> "LEFT_LEG";
                case RIGHT_LEG -> "RIGHT_LEG";
                case LOWER -> "LEFT_LEG";
                default -> "BODY";
            };
        };
    }

    private static boolean isRight(HumanoidHurtboxLibrary.Part part) {
        return part == HumanoidHurtboxLibrary.Part.RIGHT_ARM || part == HumanoidHurtboxLibrary.Part.RIGHT_LEG;
    }

    private static void logReflectionFailure(Throwable exception) {
        if (!reflectionFailureLogged) {
            reflectionFailureLogged = true;
            KingdomComeCombat.LOGGER.warn("First Aid is present, but its locational damage API is incompatible.", exception);
        }
    }

    private record TargetPart(String partName, float damageScale) {
    }

    @FunctionalInterface
    public interface Scope extends AutoCloseable {
        Scope NOOP = () -> { };

        @Override
        void close();
    }
}
