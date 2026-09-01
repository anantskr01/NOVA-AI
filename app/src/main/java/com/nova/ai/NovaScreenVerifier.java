package com.nova.ai;

import java.util.Locale;

/** Post-action verification against the current accessibility tree. */
public final class NovaScreenVerifier {
    private final NovaAccessibilityService accessibility;
    public NovaScreenVerifier(NovaAccessibilityService accessibility){this.accessibility=accessibility;}
    public boolean containsText(String expected){if(accessibility==null||expected==null||expected.trim().isEmpty())return false;return accessibility.screenText().toLowerCase(Locale.ROOT).contains(expected.trim().toLowerCase(Locale.ROOT));}
    public boolean containsUi(String expected){if(accessibility==null||expected==null||expected.trim().isEmpty())return false;return accessibility.uiSnapshot().toLowerCase(Locale.ROOT).contains(expected.trim().toLowerCase(Locale.ROOT));}
    public String snapshot(){return accessibility==null?"Accessibility unavailable.":accessibility.uiSnapshot();}
}
