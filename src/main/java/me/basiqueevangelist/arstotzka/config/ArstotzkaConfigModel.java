package me.basiqueevangelist.arstotzka.config;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Modmenu;

@Config(name = "arstotzka", wrapperName = "ArstotzkaConfig")
@Modmenu(modId = "arstotzka")
public class ArstotzkaConfigModel {
    public boolean waitingRoom = true;
    public boolean joinQueue = true;
}
