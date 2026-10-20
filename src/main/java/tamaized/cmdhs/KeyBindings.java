package tamaized.cmdhs;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import tamaized.beanification.Component;
import tamaized.beanification.PostConstruct;

@Component(dist = Dist.CLIENT)
public class KeyBindings {

	private final KeyMapping.Category category = new KeyMapping.Category(Identifier.fromNamespaceAndPath(CommandHistorySearch.MODID, "history_search"));
	private final KeyMapping reverseSearch = create("search_reverse", InputConstants.KEY_R);
	private final KeyMapping forwardSearch = create("search_forward", InputConstants.KEY_S);
	private final KeyMapping abortSearch = create("search_abort", InputConstants.KEY_G);

	@PostConstruct
	private void postConstruct(IEventBus bus) {
		bus.addListener(RegisterKeyMappingsEvent.class, event -> {
			event.registerCategory(category);
			event.register(reverseSearch);
			event.register(forwardSearch);
			event.register(abortSearch);
		});
	}

	private KeyMapping create(String name, int key) {
		return new KeyMapping(
			Identifier.fromNamespaceAndPath(CommandHistorySearch.MODID, name).toLanguageKey("key"),
			KeyConflictContext.GUI,
			KeyModifier.CONTROL,
			InputConstants.Type.KEYSYM,
			key,
			category
		);
	}

	public KeyMapping getReverseSearch() {
		return reverseSearch;
	}

	public KeyMapping getForwardSearch() {
		return forwardSearch;
	}

	public KeyMapping getAbortSearch() {
		return abortSearch;
	}

}
