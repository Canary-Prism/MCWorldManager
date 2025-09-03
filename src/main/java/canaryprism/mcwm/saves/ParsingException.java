package canaryprism.mcwm.saves;

public class ParsingException extends Exception {
    private final String verdict;
    public String getVerdict() {
        return verdict;
    }
    
    public ParsingException(String message, String verdict) {
        super(message);
        this.verdict = verdict;
    }
    public ParsingException(String message, Throwable cause, String verdict) {
        super(message, cause);
        this.verdict = verdict;
    }
    @SuppressWarnings("unused")
    public ParsingException(Throwable cause, String verdict) {
        super(cause);
        this.verdict = verdict;
    }
}
