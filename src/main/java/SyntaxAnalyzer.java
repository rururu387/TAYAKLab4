import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SyntaxAnalyzer {

    enum SymType {
        TER,
        NON_TER,
        SPEC
    }

    Map<Symbol, Set<Production>> grammars;
    Set<Symbol> non_terminal_alphabet;
    Set<Symbol> terminal_alphabet;

    public SyntaxAnalyzer(String filename) {
        grammars = new HashMap<>();
        non_terminal_alphabet = new HashSet<>();
        terminal_alphabet = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            for (String line; (line = br.readLine()) != null; ) {
                String bufferSymbol = "";
                SymType symType = null;
                Symbol grammarKey = null;
                Set<Production> grammarValue = new HashSet<>();
                Production production = new Production();
                for (int i = 0; i < line.length(); i++) {
                    switch (line.charAt(i)) {
                        case '<' -> {

                            if (symType == null)
                                symType = SymType.NON_TER;
                            else if (symType.equals(SymType.TER)) {
                                bufferSymbol += "<";
                            }
                        }
                        case '>' -> {
                            if (symType != null && symType.equals(SymType.NON_TER)) {
                                Symbol symbol = new Symbol(false, bufferSymbol);
                                non_terminal_alphabet.add(symbol);
                                if (grammarKey == null)
                                    grammarKey = symbol;
                                else
                                    production.addSymbolToProduction(symbol);
                                symType = null;
                                bufferSymbol = "";
                            } else if (symType != null && symType.equals(SymType.TER))
                                bufferSymbol += ">";
                        }
                        case ':' -> {
                            if (symType == null)
                                symType = SymType.SPEC;
                            else if (symType.equals(SymType.TER)) {
                                bufferSymbol += ":";
                            }
                        }
                        case '\'' -> {
                            if (symType == null) {
                                symType = SymType.TER;

                            } else if (symType.equals(SymType.TER)) {
                                Symbol symbol = new Symbol(true, bufferSymbol);
                                terminal_alphabet.add(symbol);
                                production.addSymbolToProduction(symbol);
                                bufferSymbol = "";
                                symType = null;
                            }
                        }
                        case ' ' -> {
                            Symbol symbol = new Symbol(true, " ");
                            non_terminal_alphabet.add(symbol);
                            production.addSymbolToProduction(symbol);
                            bufferSymbol = "";
                            symType = null;
                        }
                        case '|' -> {
                            if (symType != null && symType.equals(SymType.SPEC)) {
                                Symbol symbol = new Symbol(true, "");
                                production.addSymbolToProduction(symbol);
                                bufferSymbol = "";
                                symType = null;
                            }
                            grammarValue.add(production);
                            production = new Production();
                        }
                        default -> bufferSymbol += line.charAt(i);
                    }
                }
                grammarValue.add(production);
                grammars.put(grammarKey, grammarValue);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String grammarsToString() {
        StringBuilder retVal = new StringBuilder();
        for (var entry : grammars.entrySet()) {
            retVal.append(entry.getKey().toString());
            retVal.append(":");
            for (var production : entry.getValue()) {
                retVal.append(production.toString());
                retVal.append("|");
            }
            retVal.deleteCharAt(retVal.length() - 1);
            retVal.append("\n");
        }
        return retVal.toString();
    }
}
