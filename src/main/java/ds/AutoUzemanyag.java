package ds;

import java.util.Set;

public enum AutoUzemanyag {
    PETROL(Set.of(KutUzemanyag.PETROL_95, KutUzemanyag.PETROL_100)),
    DIESEL(Set.of(KutUzemanyag.DIESEL)),
    HYBRID(Set.of(KutUzemanyag.PETROL_95, KutUzemanyag.PETROL_100));

    private Set<KutUzemanyag> tankolhato;

    AutoUzemanyag(Set<KutUzemanyag> tankolhato) {
        this.tankolhato = tankolhato;
    }

    public boolean tankolhato(KutUzemanyag uzemanyag) {
        return this.tankolhato.contains(uzemanyag);

    }
}
