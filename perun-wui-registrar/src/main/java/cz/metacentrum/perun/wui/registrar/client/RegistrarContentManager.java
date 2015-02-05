package cz.metacentrum.perun.wui.registrar.client;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.History;
import cz.metacentrum.perun.wui.client.resources.PerunContentManager;
import cz.metacentrum.perun.wui.client.resources.PerunContextListener;
import cz.metacentrum.perun.wui.client.resources.Translatable;
import cz.metacentrum.perun.wui.pages.NotAuthorizedPage;
import cz.metacentrum.perun.wui.pages.NotFoundPage;
import cz.metacentrum.perun.wui.pages.Page;
import cz.metacentrum.perun.wui.registrar.pages.AppDetailPage;
import cz.metacentrum.perun.wui.registrar.pages.AppsPage;
import cz.metacentrum.perun.wui.registrar.pages.FormPage;
import cz.metacentrum.perun.wui.registrar.pages.LogoutPage;
import cz.metacentrum.perun.wui.widgets.PerunLoader;
import org.gwtbootstrap3.client.ui.html.Div;

import java.util.ArrayList;

/**
 * Class for managing the content of Registrar App.
 *
 * @author Pavel Zlámal <zlamal@cesnet.cz>
 */
public class RegistrarContentManager extends Div implements PerunContentManager {

	private NotFoundPage notFoundPage = new NotFoundPage();
	private NotAuthorizedPage notAuthorizedPage = new NotAuthorizedPage();
	private LogoutPage logoutPage = new LogoutPage();

	private Page displayedPage;
	private RegistrarContentManager contentManager = this;
	private PerunContextListener[] contextListeners = new PerunContextListener[1];

	private String lastContext = "";

	private ArrayList<Page> history = new ArrayList<Page>();
	private static boolean changePageActive = false;

	/**
	 * Create instance of content manager, which handle all actions related to changing the page.
	 *
	 * @param contextListeners classes, which listens to the changes of context
	 */
	public RegistrarContentManager(PerunContextListener... contextListeners) {

		if (contextListeners != null) this.contextListeners = contextListeners;
		setId("web-content");

	}

	@Override
	public void openPage(final String sourceContext) {

		String context = sourceContext;

		if (sourceContext == null || sourceContext.isEmpty()) {
			context = "form";
			History.newItem(context);
			return;
		}

		final String finalContext = context;

		Scheduler.get().scheduleIncremental(new Scheduler.RepeatingCommand() {
			@Override
			public boolean execute() {

				if (changePageActive) return true;

				changePageActive = true;

				if ("notfound".equals(finalContext)) {

					openPage(notFoundPage, true);

				} else if ("notauthorized".equals(finalContext)) {

					openPage(notAuthorizedPage, true);

				} else if ("form".equals(finalContext)) {

					openPage(new FormPage(), true);

				} else if ("submitted".equals(finalContext)) {

					openPage(new AppsPage(), true);

				} else if (finalContext.startsWith("detail")) {

					// FIXME - this is hack - we need more error proof URL parser
					String[] params = finalContext.split("\\?");
					if (params.length > 1) {
						String[] vars = params[1].split("=");
						if (vars.length > 0) {
							openPage(new AppDetailPage(Integer.valueOf(vars[1])), true);
						}
					}

				} else if ("logout".equals(finalContext)) {

					openPage(logoutPage, true);

				} else {

					// unknown page - redirect to notfound
					openPage(notFoundPage, true);

				}

				return false;

			}
		});

	}

	@Override
	public void openPage(String context, Object[]... objects) {

	}

	@Override
	public void openPage(final Page page) {
		openPage(page, false);
	}

	@Override
	public void openPage(final Page pageToShow, final boolean force) {

		// FIXME & TODO - get/store page from history
		// TODO - handle also context to be passed to open() method

		Scheduler.get().scheduleIncremental(new Scheduler.RepeatingCommand() {
			@Override
			public boolean execute() {

				if (changePageActive && !force) return true;

				changePageActive = true;

				// if already displayed
				if (displayedPage != null && displayedPage.equals(pageToShow)) {

					displayedPage.open();
					displayedPage.onResize();
					// translate
					if (displayedPage instanceof Translatable) {
						((Translatable)displayedPage).changeLanguage();
					}
					changePageActive = false;

				} else if (history.contains(pageToShow)) {

					// get page from history if possible
					for (Page p : history) {
						if (p.equals(pageToShow)) {
							contentManager.clear();
							displayedPage = p;
							contentManager.add(displayedPage.getWidget());
							displayedPage.open();
							displayedPage.onResize();
							// translate
							if (displayedPage instanceof Translatable) {
								((Translatable)displayedPage).changeLanguage();
							}
							changePageActive = false;

							// update context by page
							for (PerunContextListener listener : contextListeners) {
								listener.setContext(pageToShow.getUrl());
							}

							break;
						}
					}

				} else {

					// display page when all data all loaded (check every 200ms)
					Scheduler.get().scheduleIncremental(new Scheduler.RepeatingCommand() {

						boolean firstRun = true;

						@Override
						public boolean execute() {

							// wait for loading data
							if (!pageToShow.isPrepared()) {
								if (firstRun) {
									PerunLoader loader = new PerunLoader();
									contentManager.clear();
									contentManager.add(loader);
									loader.onLoading();
									firstRun = false;
								}
								return true;
							}

							if (pageToShow.isAuthorized()) {

								// data loaded
								displayedPage = pageToShow;
								contentManager.clear();
								contentManager.add(pageToShow.draw());
								displayedPage.open();
								displayedPage.onResize();
								// translate
								if (displayedPage instanceof Translatable) {
									((Translatable) displayedPage).changeLanguage();
								}

								// update context by page
								for (PerunContextListener listener : contextListeners) {
									listener.setContext(pageToShow.getUrl());
								}

								history.add(pageToShow);
								changePageActive = false;

								return false;

							} else {

								// not authorized to view page
								displayedPage = notAuthorizedPage;
								contentManager.clear();
								contentManager.add(displayedPage.draw());
								displayedPage.open();
								displayedPage.onResize();
								// translate
								if (displayedPage instanceof Translatable) {
									((Translatable) displayedPage).changeLanguage();
								}
								changePageActive = false;

								return false;

							}

						}
					});

				}

				return false;

			}
		});

	}

	@Override
	public Page getDisplayedPage() {
		return displayedPage;
	}

	@Override
	public void onResize() {

		if (getDisplayedPage() != null) {
			getDisplayedPage().onResize();
		}

	}

}
