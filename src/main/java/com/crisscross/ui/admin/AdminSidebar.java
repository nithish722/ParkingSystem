package com.crisscross.ui.admin;

import com.crisscross.ui.ScreenManager;

/**
 * Backward compatibility wrapper for AdminHorizontalNav.
 * The admin navigation menu is now displayed in the top horizontal panel.
 */
public class AdminSidebar extends AdminHorizontalNav {
    public AdminSidebar(ScreenManager screenManager) {
        super(screenManager, "");
    }

    public AdminSidebar(ScreenManager screenManager, String activeNavId) {
        super(screenManager, activeNavId);
    }
}
