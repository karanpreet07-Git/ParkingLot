import java.util.*;

// --- ENUM ---
enum VehicleType { BIKE, CAR, TRUCK }

// --- VEHICLE ---
abstract class Vehicle {
    String licenseNumber;
    VehicleType type;

    Vehicle(String licenseNumber, VehicleType type) {
        this.licenseNumber = licenseNumber;
        this.type = type;
    }
}

class Car extends Vehicle {
    Car(String license) { super(license, VehicleType.CAR); }
}

class Bike extends Vehicle {
    Bike(String license) { super(license, VehicleType.BIKE); }
}

class Truck extends Vehicle {
    Truck(String license) { super(license, VehicleType.TRUCK); }
}

// --- PARKING SPOT ---
class ParkingSpot {
    int id;
    boolean isOccupied;
    VehicleType type;
    Vehicle currentVehicle;

    ParkingSpot(int id, VehicleType type) {
        this.id = id;
        this.type = type;
    }

    boolean canFitVehicle(Vehicle v) {
        if (isOccupied) return false;

        if (v.type == VehicleType.BIKE) return true;
        if (v.type == VehicleType.CAR) return this.type != VehicleType.BIKE;
        if (v.type == VehicleType.TRUCK) return this.type == VehicleType.TRUCK;

        return false;
    }

    void park(Vehicle v) {
        currentVehicle = v;
        isOccupied = true;
    }

    void leave() {
        currentVehicle = null;
        isOccupied = false;
    }
}

// --- FLOOR ---
class ParkingFloor {
    int id;
    List<ParkingSpot> spots;

    ParkingFloor(int id, List<ParkingSpot> spots) {
        this.id = id;
        this.spots = spots;
    }

    // NEAREST SPOT = lowest ID
    ParkingSpot getAvailableSpot(Vehicle v) {
        return spots.stream()
                .filter(s -> s.canFitVehicle(v))
                .min(Comparator.comparingInt(s -> s.id))
                .orElse(null);
    }

    long getFreeSpots() {
        return spots.stream().filter(s -> !s.isOccupied).count();
    }
}

// --- PARKING LOT ---
class ParkingLot {
    List<ParkingFloor> floors;

    ParkingLot(List<ParkingFloor> floors) {
        this.floors = floors;
    }

    ParkingSpot findSpot(Vehicle v) {
        ParkingSpot best = null;

        for (ParkingFloor floor : floors) {
            ParkingSpot spot = floor.getAvailableSpot(v);
            if (spot != null) {
                if (best == null || spot.id < best.id) {
                    best = spot;
                }
            }
        }
        return best;
    }
}

// --- TICKET ---
class Ticket {
    static int counter = 1001;
    int ticketId;
    long entryTime;
    ParkingSpot spot;

    Ticket(ParkingSpot spot) {
        this.ticketId = counter++;
        this.entryTime = System.currentTimeMillis();
        this.spot = spot;
    }
}

// --- PRICING ---
interface PricingStrategy {
    double calculate(long duration);
}

class HourlyPricingStrategy implements PricingStrategy {
    public double calculate(long duration) {
        double hours = Math.ceil(duration / (1000.0 * 60 * 60));
        return hours * 20;
    }
}

// --- MANAGER (NEW - IMPORTANT) ---
class ParkingManager {
    ParkingLot lot;
    PricingStrategy pricing;

    Map<Integer, Ticket> activeTickets = new HashMap<>();
    Map<String, Ticket> activeVehicles = new HashMap<>();

    ParkingManager(ParkingLot lot, PricingStrategy pricing) {
        this.lot = lot;
        this.pricing = pricing;
    }

    void displayBoard() {
        System.out.println("\n--- Display Board ---");
        for (ParkingFloor floor : lot.floors) {
            System.out.println("Floor " + floor.id + " Free Spots: " + floor.getFreeSpots());
        }
    }

    void parkVehicle(Vehicle v) {
        if (activeVehicles.containsKey(v.licenseNumber)) {
            System.out.println("Already parked!");
            return;
        }

        ParkingSpot spot = lot.findSpot(v);
        if (spot == null) {
            System.out.println("No space available!");
            return;
        }

        spot.park(v);
        Ticket t = new Ticket(spot);

        activeTickets.put(t.ticketId, t);
        activeVehicles.put(v.licenseNumber, t);

        System.out.println("Parked! Ticket ID: " + t.ticketId);
    }

    void exitVehicle(int ticketId) {
        if (!activeTickets.containsKey(ticketId)) {
            System.out.println("Invalid Ticket!");
            return;
        }

        Ticket t = activeTickets.get(ticketId);
        long duration = System.currentTimeMillis() - t.entryTime;

        double cost = pricing.calculate(duration);

        String license = t.spot.currentVehicle.licenseNumber;

        t.spot.leave();
        activeTickets.remove(ticketId);
        activeVehicles.remove(license);

        System.out.println("Exit done. Charges: ₹" + cost);
    }

    void showVehicles() {
        if (activeVehicles.isEmpty()) {
            System.out.println("No vehicles parked.");
            return;
        }

        for (String license : activeVehicles.keySet()) {
            Ticket t = activeVehicles.get(license);
            System.out.println("License: " + license +
                    " | Spot: " + t.spot.id +
                    " | Type: " + t.spot.currentVehicle.type);
        }
    }
}

// --- MAIN ---
public class ParkingLotLLd {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // MULTIPLE FLOORS
        List<ParkingSpot> f1 = List.of(
                new ParkingSpot(101, VehicleType.CAR),
                new ParkingSpot(102, VehicleType.BIKE)
        );

        List<ParkingSpot> f2 = List.of(
                new ParkingSpot(201, VehicleType.TRUCK),
                new ParkingSpot(202, VehicleType.CAR)
        );

        ParkingFloor floor1 = new ParkingFloor(1, new ArrayList<>(f1));
        ParkingFloor floor2 = new ParkingFloor(2, new ArrayList<>(f2));

        ParkingLot lot = new ParkingLot(List.of(floor1, floor2));
        ParkingManager manager = new ParkingManager(lot, new HourlyPricingStrategy());

        while (true) {
            System.out.println("\n1.Park 2.Exit 3.Show 4.Display 5.Quit");
            int ch = sc.nextInt();

            if (ch == 1) {
                System.out.print("License: ");
                String lic = sc.next();

                System.out.print("Type (1 Car, 2 Bike, 3 Truck): ");
                int t = sc.nextInt();

                Vehicle v = switch (t) {
                    case 1 -> new Car(lic);
                    case 2 -> new Bike(lic);
                    case 3 -> new Truck(lic);
                    default -> null;
                };

                if (v != null) manager.parkVehicle(v);

            } else if (ch == 2) {
                System.out.print("Ticket ID: ");
                manager.exitVehicle(sc.nextInt());

            } else if (ch == 3) {
                manager.showVehicles();

            } else if (ch == 4) {
                manager.displayBoard();

            } else {
                break;
            }
        }
        sc.close();
    }
}