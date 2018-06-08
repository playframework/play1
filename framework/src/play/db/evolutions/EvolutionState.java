package play.db.evolutions;

public enum EvolutionState {
    APPLIED, APPLYING_UP, APPLYING_DOWN;

    public String getStateWord() {
        return this.name().toLowerCase();
    }
}
