import java.util.Objects;

public class Symbol {
    boolean isTerminal;
    String value;

    public String getValue() {
        return value;
    }

    public boolean isTerminal() {
        return isTerminal;
    }

    public Symbol(boolean isTerminal, String value) {
        this.isTerminal = isTerminal;
        this.value = value;
    }

    public String toString() {
        if (!isTerminal) {
            return "<" + value + ">";
        }
        if (value.equals(""))
            return "#eps#";
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Symbol symbol = (Symbol) o;
        return isTerminal == symbol.isTerminal && value.equals(symbol.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isTerminal, value);
    }
}
