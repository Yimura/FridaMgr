package sh.damon.fridamgr.filter;

import android.text.InputFilter;
import android.text.Spanned;

public class Uint16Filter implements InputFilter {
    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        String input = source.toString();
        if (input.isEmpty()) {
            return null;
        }

        int num = Integer.parseInt(input);
        if (num < 0 || num > 65_535) {
            return "";
        }
        return null;
    }
}
