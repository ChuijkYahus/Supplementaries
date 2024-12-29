package net.mehvahdjukaar.supplementaries.common.block.tiles;

import net.mehvahdjukaar.moonlight.api.client.IScreenProvider;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.supplementaries.Supplementaries;
import net.mehvahdjukaar.supplementaries.client.screens.SpeakerBlockScreen;
import net.mehvahdjukaar.supplementaries.common.block.IOnePlayerInteractable;
import net.mehvahdjukaar.supplementaries.common.block.blocks.SpeakerBlock;
import net.mehvahdjukaar.supplementaries.common.network.ClientBoundPlaySpeakerMessagePacket;
import net.mehvahdjukaar.supplementaries.configs.CommonConfigs;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.mehvahdjukaar.supplementaries.reg.ModTextures;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SpeakerBlockTile extends BlockEntity implements Nameable, IOnePlayerInteractable, IScreenProvider {
    private Component message = Component.empty();
    private Component filteredMessage = Component.empty();
    private Mode mode = Mode.CHAT;
    //distance in blocks
    private double volume = CommonConfigs.Redstone.SPEAKER_RANGE.get();
    private Component customName;
    @Nullable
    private UUID playerWhoMayEdit = null;

    public Object ccHack = null;

    public SpeakerBlockTile(BlockPos pos, BlockState state) {
        super(ModRegistry.SPEAKER_BLOCK_TILE.get(), pos, state);
    }

    public void setCustomName(Component name) {
        this.customName = name;
    }

    @Override
    public Component getName() {
        return this.customName != null ? this.customName : this.getDefaultName();
    }

    @Override
    public Component getCustomName() {
        return this.customName;
    }

    public Component getDefaultName() {
        return Component.translatable("gui.supplementaries.speaker_block");
    }

    public double getVolume() {
        return volume;
    }

    public Mode getMode() {
        return mode;
    }

    public Component getMessage(boolean filtered) {
        return filtered ? filteredMessage : message;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public void setMessage(Component message) {
        this.setMessage(message, message);
    }

    public void setMessage(Component message, Component filteredMessage) {
        this.message = message;
        this.filteredMessage = filteredMessage;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("CustomName", 8)) {
            this.customName = Component.Serializer.fromJson(tag.getString("CustomName"), registries);
        }

        this.message = Component.Serializer.fromJson(tag.getString("Message"), registries);
        if (tag.contains("FilteredMessage")) {
            this.filteredMessage = Component.Serializer.fromJson(tag.getString("FilteredMessage"), registries);
        } else filteredMessage = message;
        var m = Mode.values()[tag.getInt("Mode")];
        if (m == Mode.NARRATOR && !CommonConfigs.Redstone.SPEAKER_NARRATOR.get()) m = Mode.CHAT;
        this.mode = m;
        this.volume = tag.getDouble("Volume");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.customName != null) {
            tag.putString("CustomName", Component.Serializer.toJson(this.customName, registries));
        }
        tag.putString("Message", Component.Serializer.toJson(this.message, registries));
        if (this.message != this.filteredMessage) {
            tag.putString("FilteredMessage", Component.Serializer.toJson(this.filteredMessage, registries));
        }
        tag.putInt("Mode", this.mode.ordinal());
        tag.putDouble("Volume", this.volume);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        var name = componentInput.get(DataComponents.CUSTOM_NAME);
        if (name != null) {
            this.customName = name;
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
    }

    public void sendMessage() {
        BlockState state = this.getBlockState();

        if (level instanceof ServerLevel server && !this.message.equals("")) {
            // particle
            BlockPos pos = this.getBlockPos();
            level.blockEvent(pos, this.getBlockState().getBlock(), 0, 0);

            Style style = !state.getValue(SpeakerBlock.ANTIQUE) ? Style.EMPTY.applyFormats(ChatFormatting.ITALIC) :
                    Style.EMPTY.withFont(ModTextures.ANTIQUABLE_FONT).applyFormats(ChatFormatting.ITALIC);

            String name = this.getName().getString();
            String s = "";
            if (name.isEmpty()) {
                s = "Speaker Block: ";
            } else if (!name.equals("\"\"") && !name.equals("\"")) s += name + ": ";
            Component component = Component.literal(s + this.message.getString()).withStyle(style);
            Component filtered = Component.literal(s + this.filteredMessage.getString()).withStyle(style);

            NetworkHelper.sendToAllClientPlayersInRange(server, pos,
                    this.volume, new ClientBoundPlaySpeakerMessagePacket(component, filtered, this.mode));

        }
    }

    //needed since we access tile directly on client when opening gui
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    public boolean tryAcceptingClientText(ServerPlayer player, FilteredText filteredText) {
        if (this.isEditingPlayer(player)) {
            this.acceptClientMessages(player, filteredText);
            this.setPlayerWhoMayEdit(null);
            return true;
        } else {
            Supplementaries.LOGGER.warn("Player {} just tried to change non-editable speaker block",
                    player.getName().getString());
        }
        return false;
    }

    //takes text filtering into account
    private void acceptClientMessages(Player player, FilteredText filteredText) {
        Style style = this.getMessage(player.isTextFilteringEnabled()).getStyle();
        if (player.isTextFilteringEnabled()) {
            this.setMessage(Component.literal(filteredText.filteredOrEmpty()).setStyle(style));
        } else {
            this.setMessage(Component.literal(filteredText.raw()).setStyle(style), Component.literal(filteredText.filteredOrEmpty()).setStyle(style));
        }
    }

    @Override
    public void setPlayerWhoMayEdit(UUID playerWhoMayEdit) {
        this.playerWhoMayEdit = playerWhoMayEdit;
    }

    @Override
    public UUID getPlayerWhoMayEdit() {
        return playerWhoMayEdit;
    }

    @Override
    public void openScreen(Level level, BlockPos pos, Player player, Direction direction) {
        SpeakerBlockScreen.open(this);
    }

    public enum Mode {
        CHAT,
        STATUS_MESSAGE,
        TITLE,
        NARRATOR
    }

}