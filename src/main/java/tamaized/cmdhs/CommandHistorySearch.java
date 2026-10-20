package tamaized.cmdhs;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import tamaized.beanification.BeanContext;

@Mod(value = CommandHistorySearch.MODID, dist = Dist.CLIENT)
public class CommandHistorySearch {

	public static final String MODID = "cmdhs";

	static {
		BeanContext.init(MODID);
	}

}
