import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Emulator {

    static final int START = 1;  // Interner Startzustand (angezeigt als q0)


    // =========================================================================
    // Simulation
    // =========================================================================

    static void simulate(Map<String, Transition> table,
                         Map<Integer, Integer> inputTape,
                         boolean stepMode,
                         Scanner sc) {
        Map<Integer, Integer> tape = new HashMap<>(inputTape);
        int state = START;
        int head  = 0;
        int steps = 0;

        TMDisplay.printSimHeader(tape, stepMode);
        if (stepMode) TMDisplay.promptStep(tape, head, state, steps, sc);

        while (true) {
            int symbol = tape.getOrDefault(head, TMCore.BLANK);
            Transition t = table.get(state + "," + symbol);

            if (t == null) {
                System.out.printf("%n[HALT]  Kein Uebergang fuer (q%d, %c) nach %d Schritten.%n%n",
                        state - 1, TMCore.symChar(symbol), steps);
                break;
            }

            tape.put(head, t.writeSymbol());
            head  += (t.direction() == 2 ? 1 : -1);
            state  = t.nextState();
            steps++;

            if (stepMode) TMDisplay.promptStep(tape, head, state, steps, sc);
        }

        TMDisplay.printResult(tape, state, steps);
        TMDisplay.printBand(tape, head, state, steps);
    }


    // =========================================================================
    // Dialog / Hauptprogramm
    // =========================================================================

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setIn(new java.io.BufferedInputStream(System.in));
        Scanner sc = new Scanner(System.in, StandardCharsets.UTF_8);

        while (true) {
            System.out.println(TMDisplay.LINE2);
            System.out.println("   Turing-Maschinen-Emulator  —  THIN Aufgabe 1 & 2");
            System.out.println(TMDisplay.LINE2);

            // --- TM-Kodierung einlesen ---
            System.out.println("\nTM-Kodierung eingeben");
            System.out.println("  (oder 'T1'/'T2' fuer Vorlesungsbeispiele, 'T3' fuer Aufgabe 2):");
            System.out.print("  > ");
            String rawCode = sc.nextLine().strip();

            String tmCode = switch (rawCode.toUpperCase()) {
                case "T1" -> { System.out.println("  T1 = " + TMCore.T1); yield TMCore.T1; }
                case "T2" -> { System.out.println("  T2 = " + TMCore.T2); yield TMCore.T2; }
                case "T3" -> { System.out.println("  T3 = " + TMCore.T3); yield TMCore.T3; }
                default   -> rawCode;
            };

            System.out.println();
            Map<String, Transition> table = TMCore.parseTM(tmCode);
            if (table.isEmpty()) {
                System.out.println("Fehler: Keine gueltigen Uebergaenge gefunden.");
                continue;
            }
            TMDisplay.printTransitions(table);

            // --- Bandeingabe ---
            System.out.println("\nEingabe auf dem Band (Dezimalzahl):");
            System.out.print("  > ");
            String rawInput = sc.nextLine().strip();

            System.out.println("Kodierung: (b)inaer oder (u)naer? [b/u]");
            System.out.print("  > ");
            boolean unary = sc.nextLine().strip().equalsIgnoreCase("u");

            if (!rawInput.isEmpty()) {
                try {
                    long n = Long.parseLong(rawInput);
                    if (unary) {
                        rawInput = "1".repeat((int) n);
                        System.out.printf("  Dezimal -> Unaer:  %s%n", rawInput);
                    } else {
                        rawInput = Long.toBinaryString(n);
                        System.out.printf("  Dezimal -> Binaer: %s%n", rawInput);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("  [!] Ungueltige Eingabe — wird als leer behandelt.");
                    rawInput = "";
                }
            }

            // --- Modus waehlen ---
            System.out.println("\nModus: (s)chritt-Modus  oder  (l)auf-Modus? [s/l]");
            System.out.print("  > ");
            boolean stepMode = sc.nextLine().strip().equalsIgnoreCase("s");

            // --- Simulation starten ---
            simulate(table, TMCore.parseTape(rawInput), stepMode, sc);

            // --- Neustart oder Beenden ---
            System.out.println("\n" + TMDisplay.LINE2);
            System.out.println("  [R] Neustart   |   beliebige Taste + ENTER zum Beenden");
            System.out.print("  > ");
            if (!sc.nextLine().strip().equalsIgnoreCase("r")) break;
            System.out.println();
        }
    }
}
