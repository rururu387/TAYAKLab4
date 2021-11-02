import java.util.ArrayList;
import java.util.List;

public class Production {
    List<Symbol> symbolList;

    public Production() {
        symbolList = new ArrayList<>();
    }

    public void addSymbolToProduction(Symbol symbol) {
        symbolList.add(symbol);
    }

    public String toString() {
        StringBuilder retVal = new StringBuilder();
        for (Symbol symbol : symbolList) {
            retVal.append(symbol.toString());
        }
        return retVal.toString();
    }
}
