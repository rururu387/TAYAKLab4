import java.util.ArrayList;
import java.util.List;

public class Production {
    List<Symbol> symbolList;
    boolean synchro_error = false;


    public Production() {
        symbolList = new ArrayList<>();
    }

    public  Production(boolean synchro_error){
        this.synchro_error = true;
    }

    public void addSymbolToProduction(Symbol symbol) {
        symbolList.add(symbol);
    }

    public String toString() {
        if (synchro_error){
            return "SYNCHRO_ERROR";
        }
        StringBuilder retVal = new StringBuilder();
        for (Symbol symbol : symbolList) {
            retVal.append(symbol.toString());
        }
        return retVal.toString();
    }

    public Symbol getEps(){
        if (symbolList.size()!= 1 )
            return null;
        Symbol eps = first();
        if (eps.value.equals(""))
            return eps;
        return null;
    }

    public boolean isEps(){
       return getEps()!=null;
    }

    public Symbol first(){
        return symbolList.get(0);
    }
}
