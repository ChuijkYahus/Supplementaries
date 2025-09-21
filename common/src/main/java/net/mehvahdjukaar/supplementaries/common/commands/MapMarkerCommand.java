package net.mehvahdjukaar.supplementaries.common.commands;


import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.mehvahdjukaar.moonlight.api.map.MapDataRegistry;
import net.mehvahdjukaar.moonlight.api.map.MapHelper;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;


public class MapMarkerCommand {

    public static ArgumentBuilder<CommandSourceStack, ?> register(CommandBuildContext context) {
        return Commands.literal("add_marker")
                .requires(cs -> cs.hasPermission(2))
                .then(Commands.argument("marker", ResourceArgument.resource(context, MapDataRegistry.MAP_DECORATION_REGISTRY_KEY))
                        .executes(MapMarkerCommand::addMapMarker)
                );
    }


    public static int addMapMarker(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        var decoration = ResourceArgument.getResource(context, "marker", MapDataRegistry.MAP_DECORATION_REGISTRY_KEY);
        var p = source.getPlayer();
        if (p != null) {
            ItemStack stack = p.getMainHandItem();
            var data = MapHelper.getMapData(stack, level, p);
            if (data != null) {
                MapHelper.addCustomTargetDecorationToItem(stack, p.getOnPos(), decoration, 0);
            }
        }
        return 0;
    }
}