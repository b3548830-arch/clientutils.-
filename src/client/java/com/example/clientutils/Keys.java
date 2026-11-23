package com.example.clientutils;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/** Опрос клавиш напрямую через GLFW (не зависит от версии API ввода Minecraft). */
public final class Keys {
    private Keys() {}

    public static boolean down(MinecraftClient mc, int key) {
        return key > 0 && GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
    }

    public static String name(int k) {
        if (k <= 0) return "не назначено";
        if (k >= 290 && k <= 314) return "F" + (k - 289);
        if (k >= 320 && k <= 329) return "Num " + (k - 320);
        switch (k) {
            case 32: return "Пробел";
            case 256: return "Esc";
            case 257: return "Enter";
            case 258: return "Tab";
            case 259: return "Backspace";
            case 260: return "Insert";
            case 261: return "Delete";
            case 262: return "Стрелка вправо";
            case 263: return "Стрелка влево";
            case 264: return "Стрелка вниз";
            case 265: return "Стрелка вверх";
            case 266: return "PageUp";
            case 267: return "PageDown";
            case 268: return "Home";
            case 269: return "End";
            case 280: return "CapsLock";
            case 340: return "Левый Shift";
            case 341: return "Левый Ctrl";
            case 342: return "Левый Alt";
            case 344: return "Правый Shift";
            case 345: return "Правый Ctrl";
            case 346: return "Правый Alt";
            default:
                String n = GLFW.glfwGetKeyName(k, 0);
                return n != null ? n.toUpperCase() : "Key " + k;
        }
    }
}
