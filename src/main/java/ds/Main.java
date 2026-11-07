package ds;

import ds.BenzinkutApi.Benzinkut;
import ds.BenzinkutApi.Uzemanyag;
import ds.KeszletApi.FuelStock;
import ds.KeszletApi.Station;
import ds.KolcsonzoApi.Bejelentkezes;
import ds.KolcsonzoApi.Car;
import ds.KolcsonzoApi.Location;
import ds.KolcsonzoApi.LoginKeres;

import java.util.List;
import java.util.Scanner;

public class Main {
    public static Bejelentkezes bejelentkezes;
    static Scanner in = new Scanner(System.in);

    public static void main(String[] args) throws Exception {

        bejelentkezes = Konzol.belepes();
        Konzol.listazzKolcsonzot();
        Konzol.listazzKutakat();
        Konzol.foglaljAutot(bejelentkezes.accessToken());
        Konzol.listazzFoglalasaimat(bejelentkezes.accessToken());

    }

}
