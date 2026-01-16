package net.hyvanced.Ui;

import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.BasicCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

public class solarPanel extends BasicCustomUIPage {

    public solarPanel(@Nonnull PlayerRef playerRef)
    {
        super(playerRef,CustomPageLifetime.CanDismiss);
    }

    @Override
    public void build(@Nonnull UICommandBuilder cmd){
        cmd.append("HyvancedPlugin/solarPanel.ui");
    }
}
