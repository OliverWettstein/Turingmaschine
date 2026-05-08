import java.util.*;

public class TMCore {

    static final int BLANK = 3;

    static final String T1 = "010010001010011000101010010110001001001010011000100010001010";
    static final String T2 = "1010010100100110101000101001100010010100100110001010010100";
    static final String T3 = "0100101001001101000100100001011001010010101100100100001010011001000100000010001001100001010000101001100001001000010010011000010000100001000010011000010001000001001011000001001000001001011000001000010010000101100000010100000010100110000001001000000100100110000001000010000001000010011000000100010000000100001011000000010100000001010110000000100100000001001011000000010000100000001000010110000000100010000000010001001100000000101000000000100010011000000001000010000000000000010001001100000000010100000000010100110000000001000010000000000100001001100000000001010000000000101001100000000001001000000000001010011000000000010000100000000000001000010110000000000010010000000000010010011000000000001000010000000000010000100110000000000010001000000000000100101100000000000010010000000000001001011000000000000100001000000000000100001011000000000000101000000000010100110000000000000101000000000000010010110000000000000100001000000010000101100000000000000100100000000000000100010011000000000000001000010001000100";

    static char symChar(int sym) {
        return switch (sym) {
            case 1  -> '0';
            case 2  -> '1';
            default -> '_';
        };
    }

    static Map<String, Transition> parseTM(String code) {
        code = code.strip().replace("{", "").replace("}", "").replace(" ", "");
        Map<String, Transition> table = new LinkedHashMap<>();

        for (String part : code.split("11")) {
            if (part.isEmpty()) continue;

            String[] c = part.split("1", -1);
            if (c.length != 5) {
                System.out.printf("  [!] Ungueltig (%d Teile statt 5): '%s'%n", c.length, part);
                continue;
            }

            int state  = c[0].length();
            int symbol = c[1].length();
            String key = state + "," + symbol;

            if (table.containsKey(key))
                System.out.printf("  [!] Doppelter Uebergang (q%d, X%d) — wird ueberschrieben%n", state - 1, symbol);

            table.put(key, new Transition(c[2].length(), c[3].length(), c[4].length()));
        }
        return table;
    }

    static Map<Integer, Integer> parseTape(String raw) {
        Map<Integer, Integer> tape = new HashMap<>();
        for (int i = 0; i < raw.length(); i++) {
            if      (raw.charAt(i) == '0') tape.put(i, 1);
            else if (raw.charAt(i) == '1') tape.put(i, 2);
        }
        return tape;
    }

    static String getResult(Map<Integer, Integer> tape) {
        if (tape.isEmpty()) return "";
        int lo = Collections.min(tape.keySet());
        int hi = Collections.max(tape.keySet());
        StringBuilder sb = new StringBuilder();
        for (int p = lo; p <= hi; p++) {
            int sym = tape.getOrDefault(p, BLANK);
            if (sym != BLANK) sb.append(symChar(sym));
        }
        return sb.toString();
    }
}
