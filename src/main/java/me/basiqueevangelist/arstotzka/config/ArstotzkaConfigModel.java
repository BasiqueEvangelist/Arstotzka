package me.basiqueevangelist.arstotzka.config;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Modmenu;

@Config(name = "arstotzka", wrapperName = "ArstotzkaConfig")
@Modmenu(modId = "arstotzka")
public class ArstotzkaConfigModel {
    public Mode mode = Mode.HOLD;

    public enum Mode {
        ALLOW,
        HOLD,
        DENY,
    }
}
