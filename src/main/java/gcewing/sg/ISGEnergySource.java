package gcewing.sg;

/** Something a stargate can draw energy from, in stargate energy units. */
public interface ISGEnergySource {
    double availableEnergy();

    double drawEnergy(double amount);
}
