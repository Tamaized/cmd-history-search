package tamaized.cmdhs;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ArrayListDeque;
import net.minecraft.util.CommonColors;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import tamaized.beanification.Autowired;
import tamaized.beanification.PostConstruct;

import java.util.Locale;

@tamaized.beanification.Component(dist = Dist.CLIENT)
public class ChatHistorySearch {

	@Autowired(dist = Dist.CLIENT)
	private KeyBindings keyBindings;

	private final String reversePrompt = promptKey("search_reverse");
	private final String reverseFailedPrompt = promptKey("search_reverse_failed");
	private final String forwardPrompt = promptKey("search_forward");
	private final String forwardFailedPrompt = promptKey("search_forward_failed");

	@Nullable
	private ChatScreen screen;
	private String query = "";
	private int matchIndex;
	private boolean reverse;
	private boolean failed;

	@PostConstruct(PostConstruct.Bus.GAME)
	private void postConstruct(IEventBus bus) {
		bus.addListener(this::keyPressedListener);
		bus.addListener(this::characterTypedListener);
		bus.addListener(this::renderListener);
		bus.addListener(this::initListener);
		bus.addListener(this::closingListener);
	}

	private String promptKey(String name) {
		return Identifier.fromNamespaceAndPath(CommandHistorySearch.MODID, name).toLanguageKey("gui");
	}

	private void keyPressedListener(ScreenEvent.KeyPressed.Pre event) {
		if (!(event.getScreen() instanceof ChatScreen chatScreen))
			return;
		KeyEvent keyEvent = event.getKeyEvent();
		InputConstants.Key key = InputConstants.getKey(keyEvent);
		if (keyBindings.getReverseSearch().isActiveAndMatches(key)) {
			step(chatScreen, true);
			event.setCanceled(true);
		} else if (keyBindings.getForwardSearch().isActiveAndMatches(key)) {
			step(chatScreen, false);
			event.setCanceled(true);
		} else if (chatScreen == screen && !KeyModifier.isKeyCodeModifier(key)) {
			if (keyBindings.getAbortSearch().isActiveAndMatches(key) || keyEvent.isEscape()) {
				chatScreen.moveInHistory(history().size());
				screen = null;
				event.setCanceled(true);
			} else if (keyEvent.key() == InputConstants.KEY_BACKSPACE) {
				if (!query.isEmpty()) {
					query = query.substring(0, query.offsetByCodePoints(query.length(), -1));
					search(chatScreen, reverse ? history().size() - 1 : 0, false);
				}
				event.setCanceled(true);
			} else if (!isTyping(keyEvent))
				screen = null;
		}
	}

	private boolean isTyping(KeyEvent keyEvent) {
		if (keyEvent.isSelectAll() || keyEvent.isCopy() || keyEvent.isPaste() || keyEvent.isCut())
			return false;
		return keyEvent.key() == InputConstants.KEY_SPACE || GLFW.glfwGetKeyName(keyEvent.key(), keyEvent.scancode()) != null;
	}

	private void characterTypedListener(ScreenEvent.CharacterTyped.Pre event) {
		if (screen == null || event.getScreen() != screen)
			return;
		query += Character.toString(event.getCodePoint());
		search(screen, matchIndex >= 0 && matchIndex < history().size() ? matchIndex : matchIndex + direction(), false);
		event.setCanceled(true);
	}

	private void renderListener(ScreenEvent.Render.Post event) {
		Screen chatScreen = event.getScreen();
		if (chatScreen != screen)
			return;
		Minecraft minecraft = Minecraft.getInstance();
		GuiGraphicsExtractor graphics = event.getGuiGraphics();
		graphics.fill(2, chatScreen.height - 28, chatScreen.width - 2, chatScreen.height - 14, minecraft.options.getBackgroundColor(Integer.MIN_VALUE));
		graphics.text(minecraft.font, Component.translatable(prompt(), query), 4, chatScreen.height - 26, CommonColors.WHITE);
	}

	private void initListener(ScreenEvent.Init.Post event) {
		if (event.getScreen() == screen)
			screen = null;
	}

	private void closingListener(ScreenEvent.Closing event) {
		if (event.getScreen() == screen)
			screen = null;
	}

	private void step(ChatScreen chatScreen, boolean reverseDirection) {
		if (chatScreen != screen) {
			screen = chatScreen;
			query = "";
			failed = false;
			matchIndex = reverseDirection ? history().size() : -1;
		}
		reverse = reverseDirection;
		search(chatScreen, matchIndex + direction(), true);
	}

	private void search(ChatScreen chatScreen, int from, boolean skipCurrent) {
		if (query.isEmpty()) {
			failed = false;
			return;
		}
		ArrayListDeque<String> history = history();
		@Nullable String current = matchIndex >= 0 && matchIndex < history.size() ? history.get(matchIndex) : null;
		String needle = query.toLowerCase(Locale.ROOT);
		for (int i = from; i >= 0 && i < history.size(); i += direction()) {
			String entry = history.get(i);
			if (entry.toLowerCase(Locale.ROOT).contains(needle) && !(skipCurrent && entry.equals(current))) {
				matchIndex = i;
				failed = false;
				chatScreen.moveInHistory(history.size());
				chatScreen.moveInHistory(i - history.size());
				return;
			}
		}
		failed = true;
	}

	private int direction() {
		return reverse ? -1 : 1;
	}

	private String prompt() {
		if (reverse)
			return failed ? reverseFailedPrompt : reversePrompt;
		return failed ? forwardFailedPrompt : forwardPrompt;
	}

	private ArrayListDeque<String> history() {
		return Minecraft.getInstance().gui.getChat().getRecentChat();
	}

}
