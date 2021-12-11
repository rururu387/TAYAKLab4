import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class SyntaxAnalyzer {

    enum SymType {
        TER,
        NON_TER,
        SPEC
    }

    Symbol start_symbol;
    Map<Symbol, Set<Production>> grammars;
    Set<Symbol> non_terminal_alphabet;
    Set<Symbol> terminal_alphabet;

    Map<Symbol, Set<Symbol>> firsts;
    Map<Symbol, Set<Symbol>> follows;

    Map<Symbol, Map<Symbol, Production>> symbol_matrix; //symbol_matrix.get(non_terminal).get(terminal) -> production
    Map<Symbol, Set<Symbol>> synchro_map;

    public SyntaxAnalyzer(String filename) {
        grammars = new LinkedHashMap<>();
        non_terminal_alphabet = new HashSet<>();
        terminal_alphabet = new HashSet<>();

        firsts = new HashMap<>();
        follows = new HashMap<>();

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
                            if (symType == null || symType.equals(SymType.SPEC))
                                symType = SymType.NON_TER;
                            else if (symType.equals(SymType.TER)) {
                                bufferSymbol += "<";
                            }
                        }
                        case '>' -> {
                            if (symType != null && symType.equals(SymType.NON_TER)) {
                                Symbol symbol = new Symbol(false, bufferSymbol);
                                non_terminal_alphabet.add(symbol);
                                if (start_symbol == null)
                                    start_symbol = symbol;
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
                            if (symType == null || symType.equals(SymType.SPEC)) {
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
                            terminal_alphabet.add(symbol);
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

        for (var entry : grammars.entrySet()) {
            var non_terminal = entry.getKey();
            Set<Symbol> first_set = getFirst(non_terminal);
            firsts.put(non_terminal, first_set);
        }
        fillFollow();
        fillMatrix();
        fill_synchro();
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


    private Set<Symbol> getFirst(Symbol symbol) {
        Symbol eps = new Symbol(true, "");
        boolean hasEps = false;
        boolean hasIncluded = false;
        Set<Symbol> returnSet = new HashSet<>();
        if (symbol.isTerminal) {
            returnSet.add(symbol);
            return returnSet;
        }
        for (Production pr : grammars.get(symbol)) {
            Symbol firstSymbol = pr.first();
            if (firstSymbol.isTerminal) {
                returnSet.add(firstSymbol);
                if (firstSymbol.equals(eps)) {
                    hasEps = false;
                }
            } else {
                Set<Symbol> first_from_first_symbol = getFirst(firstSymbol);
                int before_remove_size = first_from_first_symbol.size();
                first_from_first_symbol.removeIf(iterable -> iterable.equals(eps));
                if (before_remove_size == first_from_first_symbol.size()) {
                    hasEps = false;
                } else {
                    if (!hasIncluded) {
                        hasEps = true;
                    }
                }
                hasIncluded = true;
                returnSet.addAll(first_from_first_symbol);
            }
        }
        if (hasEps)
            returnSet.add(eps);
        return returnSet;
    }

    private void fillFollow() {
        Symbol eps = new Symbol(true, "");
        for (var entry : grammars.entrySet()) {
            Set<Production> production_set = entry.getValue();
            for (Production prod : production_set) {
                for (int i = 0; i < prod.symbolList.size() - 1; i++) {
                    Symbol current_symbol = prod.symbolList.get(i);
//                    if (!current_symbol.isTerminal) {
                    Symbol next_symbol = prod.symbolList.get(i + 1);
                    if (!follows.containsKey(current_symbol)) {
                        follows.put(current_symbol, new HashSet<>());
                        if (current_symbol.equals(start_symbol))
                            follows.get(current_symbol).add(eps);
                    }
                    Set<Symbol> next_symbol_firsts;
                    next_symbol_firsts = new HashSet<>();
                    if (next_symbol.isTerminal() && !next_symbol.equals(eps)) {
                        next_symbol_firsts.add(next_symbol);
                    } else {
                        next_symbol_firsts.addAll(firsts.get(next_symbol));
                        next_symbol_firsts.remove(eps);
                    }
                    follows.get(current_symbol).addAll(next_symbol_firsts);
//                    }
                }
            }
        }
        for (var entry : grammars.entrySet()) {
            Symbol non_terminal = entry.getKey();
            if (!follows.containsKey(non_terminal)) {
                follows.put(non_terminal, new HashSet<>());
                if (non_terminal.equals(start_symbol))
                    follows.get(non_terminal).add(eps);
            }
            Set<Production> production_set = entry.getValue();
            for (Production prod : production_set) {
                for (int i = 0; i < prod.symbolList.size(); i++) {
                    Symbol current_symbol = prod.symbolList.get(i);
//                    if (!current_symbol.isTerminal) {
                    if (!follows.containsKey(current_symbol)) {
                        follows.put(current_symbol, new HashSet<>());
                        if (current_symbol.equals(start_symbol))
                            follows.get(current_symbol).add(eps);
                    }
                    Set<Symbol> next_symbol_firsts = null;
                    if (i != prod.symbolList.size() - 1) {
                        Symbol next_symbol = prod.symbolList.get(i + 1);
                        if (next_symbol.isTerminal()) {
                            next_symbol_firsts = new HashSet<>();
                            next_symbol_firsts.add(next_symbol);
                        } else {
                            next_symbol_firsts = firsts.get(next_symbol);
                        }
                    }
                    if (i == prod.symbolList.size() - 1 || next_symbol_firsts.contains(eps)) {
                        follows.get(current_symbol).addAll(follows.get(non_terminal));
                    }
//                    }
                }
            }
        }
    }

    private void fillMatrix() {
        Symbol eps = new Symbol(true, "");
        symbol_matrix = new HashMap<>();
        for (Symbol non_terminal : non_terminal_alphabet) {
            symbol_matrix.put(non_terminal, new HashMap<>());
        }
        for (var entry : grammars.entrySet()) {
            Symbol non_terminal = entry.getKey();
            Set<Production> productions = entry.getValue();
            for (Production production : productions) {
                for (Symbol terminal : getFirst(production.first())) {
                    symbol_matrix.get(non_terminal).put(terminal, production);
                    if (production.symbolList.contains(eps)) {
                        for (Symbol b : follows.get(non_terminal))
                            symbol_matrix.get(non_terminal).put(b, production);
                    }
                }
            }
            for (Symbol follow : follows.get(non_terminal)) {
                if (symbol_matrix.get(non_terminal).get(follow) == null) {
                    symbol_matrix.get(non_terminal).put(follow, new Production(true));
                }
            }
        }
    }

    private void fill_synchro(){
        synchro_map = new HashMap<>();
        for(Symbol non_ter: non_terminal_alphabet){
            synchro_map.put(non_ter, new HashSet<>());
            synchro_map.get(non_ter).addAll(follows.get(non_ter));
            synchro_map.get(non_ter).addAll(firsts.get(non_ter));
        }
    }

    Symbol get_terminal_from_str(String str){
        Symbol terminal = null;
        for (Symbol symbol : terminal_alphabet) {
            if (str.startsWith(symbol.value) && !symbol.value.equals("")) {
                if (terminal == null || symbol.value.length() > terminal.value.length())
                    terminal = symbol;
            }
        }
        return terminal;
    }

    public void call(String analyzed_file) throws IOException {
        PrintWriter writer = new PrintWriter("log.csv", StandardCharsets.UTF_8);
        writer.println("Cтек,Вход,Примечание");
        Path path = Paths.get(analyzed_file);
        String file_data = String.join("", Files.readAllLines(path));
        Stack<Symbol> stack = new Stack<>();
        stack.push(start_symbol);
        Symbol terminal = null;
        String log_state;
        while (!stack.empty()) {
           log_state = "\"" + print_symbol_stack(stack) + "\",\"" + file_data + "\"";
           terminal = get_terminal_from_str(file_data);
            if (terminal != null) {
                if (stack.peek().isTerminal && stack.peek().equals(terminal)) {
                    stack.pop();
                    file_data = file_data.substring(terminal.value.length());
                    writer.println(log_state);
                }
                else{
                    if (stack.peek().isTerminal &&  !stack.peek().equals(terminal)) {
                        log_state += ",\"" + "Ошибка (Несоответствие терминалов)\"";
                        writer.println(log_state);
                        System.out.println(file_data + "->expected '" + stack.peek().value + "' " + "Stack:" + print_symbol_stack(stack));
                        stack.pop();
                        continue;
                    }
                    Production matrix_cell = symbol_matrix.get(stack.peek()).get(terminal);
                    if (matrix_cell == null) {
                        boolean can_to_eps = false;
                        for(Production pr: grammars.get(stack.peek())){
                            if(pr.isEps())
                                can_to_eps = true;
                        }
                        log_state += ",\"Ошибка: ";
                        if(can_to_eps){
                            log_state += "снимаем " + stack.peek().toString() + " со стека\"";
                            writer.println(log_state);
                            stack.pop();
                        }else{
                            while(!synchro_map.get(stack.peek()).contains(terminal)) {
                                log_state += "Удаляем со входного потока:" + terminal.value + "\"";
                                writer.println(log_state);
                                print_error(file_data, terminal.value, stack);
                                file_data = file_data.substring(terminal.value.length());
                                terminal = get_terminal_from_str(file_data);
                                log_state = "\"" + print_symbol_stack(stack) + "\",\"" + file_data + "\",\"";
                            }
                        }
                    } else {
                        if (matrix_cell.synchro_error)
                        {
                            if (stack.size() == 1) {
                                log_state += ",\"Ошибка: Удаляем со входного потока " + terminal.value + "\"";
                                writer.println(log_state);
                                print_error(file_data, terminal.value, stack);
                                file_data = file_data.substring(terminal.value.length());
                            } else {
                                log_state += ",\"СинхроОшибка: снимаем " + stack.peek().toString() + " со стека\"";
                                writer.println(log_state);
                                print_error(file_data, terminal.value, stack);
                                stack.pop();
                            }
                        } else { //все ок
                            writer.println(log_state);
                            stack.pop();
                            if (!matrix_cell.isEps()) {
                                ListIterator<Symbol> it = matrix_cell.symbolList.listIterator(matrix_cell.symbolList.size());
                                while (it.hasPrevious()) {
                                    stack.push(it.previous());
                                }
                            }
                        }
                    }
                }
            } else { // terminal == null
                if (file_data.length() > 0) {
                    log_state += ",\"Ошибка:Неопознанный символ.Удаляем со входного потока " + file_data.substring(0, 1) + "\"";
                    writer.println(log_state);
                    print_error(file_data, file_data.substring(0, 1), stack);
                    file_data = file_data.substring(1);
                }else{
                    writer.println(log_state);
                    stack.pop();
                }
            }
        }
        writer.close();
    }

    private void print_error(String file, String top_file, Stack<Symbol> stack) {
        System.out.println(file + "->Unexpected '" + top_file + "' " +"Stack:" + print_symbol_stack(stack));
    }

    private String print_symbol_stack(Stack<Symbol> stack) {
        Stack<Symbol> printable = new Stack<>();
        printable.addAll(stack);
        StringBuilder ret_val = new StringBuilder();
        while (!printable.empty()) {
            ret_val.append(printable.pop());
        }
        return ret_val.toString();
    }
}
