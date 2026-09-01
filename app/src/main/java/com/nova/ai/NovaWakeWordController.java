package com.nova.ai;

import java.util.Locale;

/** Wake-word gate for recognized speech. Continuous microphone capture remains opt-in and OS-permission bound. */
public final class NovaWakeWordController {
    private final String wakeWord;
    public NovaWakeWordController(){this("nova");}
    public NovaWakeWordController(String wakeWord){this.wakeWord=normalize(wakeWord);}
    public boolean isWakeWord(String text){String t=normalize(text);return !t.isEmpty()&&(t.equals(wakeWord)||t.startsWith(wakeWord+" ")||t.contains(" "+wakeWord+" "));}
    public String removeWakeWord(String text){if(text==null)return "";String t=text.trim();String lower=t.toLowerCase(Locale.ROOT);if(lower.equals(wakeWord))return "";if(lower.startsWith(wakeWord+" "))return t.substring(wakeWord.length()).trim();return t;}
    private static String normalize(String s){return s==null?"":s.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");}
}
