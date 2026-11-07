package com.crisscross.ui.customer;

import com.crisscross.ui.ScreenManager;

/**
 * Backward compatibility wrapper for CustomerHorizontalNav.
 * The customer navigation menu is now displayed in the top horizontal panel.
 */
public class CustomerSidebar extends CustomerHorizontalNav {
    public CustomerSidebar(ScreenManager screenManager) {
        super(screenManager, "");
    }

    public CustomerSidebar(ScreenManager screenManager, String activeNavId) {
        super(screenManager, activeNavId);
    }
}
