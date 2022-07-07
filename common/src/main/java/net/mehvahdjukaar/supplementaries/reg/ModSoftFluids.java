package net.mehvahdjukaar.supplementaries.reg;

import net.mehvahdjukaar.moonlight.api.fluids.SoftFluid;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluidRegistry;
import net.mehvahdjukaar.moonlight.api.misc.ObjectReference;
import net.mehvahdjukaar.supplementaries.Supplementaries;

public class ModSoftFluids {

    public static final ObjectReference<SoftFluid> DIRT = new ObjectReference<>(Supplementaries.res("dirt"), SoftFluidRegistry.REGISTRY_KEY);
    public static final ObjectReference<SoftFluid> SAP = new ObjectReference<>(Supplementaries.res("sap"), SoftFluidRegistry.REGISTRY_KEY);


}
