package net.mehvahdjukaar.supplementaries.reg;

import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.mehvahdjukaar.supplementaries.Supplementaries;
import net.mehvahdjukaar.supplementaries.common.inventories.*;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Supplier;

import static net.mehvahdjukaar.supplementaries.reg.ModConstants.*;

public class ModMenuTypes {

    public static void init(){
    }

    //menu
    public static final Supplier<MenuType<PresentContainerMenu>> PRESENT_BLOCK = RegHelper.registerMenuType(
            Supplementaries.res(PRESENT_NAME), PresentContainerMenu::new);

    public static final Supplier<MenuType<TrappedPresentContainerMenu>> TRAPPED_PRESENT_BLOCK = RegHelper.registerMenuType(
            Supplementaries.res(TRAPPED_PRESENT_NAME), TrappedPresentContainerMenu::new);

    public static final Supplier<MenuType<NoticeBoardContainerMenu>> NOTICE_BOARD = RegHelper.registerMenuType(
            Supplementaries.res(NOTICE_BOARD_NAME), NoticeBoardContainerMenu::new);

    public static final Supplier<MenuType<VariableSizeContainerMenu>> VARIABLE_SIZE = RegHelper.registerMenuType(
            Supplementaries.res("variable_size"), VariableSizeContainerMenu::new);

    public static final Supplier<MenuType<SafeContainerMenu>> SAFE = RegHelper.registerMenuType(
            Supplementaries.res(SAFE_NAME), SafeContainerMenu::new);

    public static final Supplier<MenuType<PulleyContainerMenu>> PULLEY_BLOCK = RegHelper.registerMenuType(
            Supplementaries.res(PULLEY_BLOCK_NAME), PulleyContainerMenu::new);

    public static final Supplier<MenuType<CannonContainerMenu>> CANNON = RegHelper.registerMenuType(
            Supplementaries.res(CANNON_NAME), CannonContainerMenu::new);

    public static final Supplier<MenuType<RedMerchantMenu>> RED_MERCHANT = RegHelper.registerMenuType(
            Supplementaries.res(RED_MERCHANT_NAME), RedMerchantMenu::new);

}
