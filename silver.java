
// System.err.println();
// Simulate upgrades (predict)
import java.util.*;
import java.io.*;
import java.math.*;
import java.util.concurrent.ThreadLocalRandom;

enum Owner {
    PLAYER,
    ENEMY,
    NEUTRAL
}

class Tools {
    public static Owner convertNumberToOwner(int owner) {
        if (owner == 1) {
            return Owner.PLAYER;
        } else if (owner == -1) {
            return Owner.ENEMY;
        } else if (owner == 0) {
            return Owner.NEUTRAL;
        } else {
            throw new java.lang.Error("Owner of type " + owner + " unkown.");
        }
    }
}

class Game {
    private ArrayList<Factory> factories = new ArrayList<Factory>();
    Factory main_factory;
    private ArrayList<Connection> connections = new ArrayList<Connection>();
    private ArrayList<Troop> troops = new ArrayList<Troop>();
    private ArrayList<Bomb> bombs = new ArrayList<Bomb>();
    private Scanner in = new Scanner(System.in);

    public Factory getFactory(int id) {
        if (id < 0) {
            throw new java.lang.Error("Id can't be smaller than 0, now it is " + id);
        }
        for (Factory factory : this.factories) {
            if (factory.getId() == id) {
                return factory;
            }
        }
        Factory factory = new Factory(id);
        this.factories.add(factory);
        return factory;
    }

    public ArrayList<Factory> getMyFactories() {
        ArrayList<Factory> factories = new ArrayList<Factory>();
        for (Factory factory : this.factories) {
            if (factory.isMyProperty()) {
                factories.add(factory);
            }
        }
        return factories;
    }

    public ArrayList<Factory> getEnemyFactories() {
        ArrayList<Factory> factories = new ArrayList<Factory>();
        for (Factory factory : this.factories) {
            if (factory.isEnemyProperty()) {
                factories.add(factory);
            }
        }
        return factories;
    }

    public Troop getTroop(int id) {
        if (id < 0) {
            throw new java.lang.Error("Id can't be smaller than 0, now it is " + id);
        }
        for (Troop troop : this.troops) {
            if (troop.getId() == id) {
                return troop;
            }
        }
        Troop troop = new Troop(id);
        this.troops.add(troop);
        return troop;
    }

    public Bomb getBomb(int id) {
        if (id < 0) {
            throw new java.lang.Error("Id can't be smaller than 0, now it is " + id);
        }
        for (Bomb bomb : this.bombs) {
            if (bomb.getId() == id) {
                return bomb;
            }
        }
        Bomb bomb = new Bomb(id);
        this.bombs.add(bomb);
        return bomb;
    }

    public Connection getConnectionBetween(Factory factory_1, Factory factory_2) {
        for (Connection connection : this.connections) {
            if (connection.connectsFactories(factory_1, factory_2)) {
                return connection;
            }
        }
        throw new java.lang.Error(
                "Connection between " + factory_1.getId() + " and " + factory_2.getId() + "doesn't exits");
    }

    public void start() {
        int factoryCount = in.nextInt();
        for (int id = 0; id < factoryCount; id++) {
            Factory factory = new Factory(id);
            this.factories.add(factory);
        }

        int linkCount = in.nextInt();
        for (int i = 0; i < linkCount; i++) {
            int factory1Id = in.nextInt();
            int factory2Id = in.nextInt();
            int distance = in.nextInt();
            Factory factory1 = this.getFactory(factory1Id);
            Factory factory2 = this.getFactory(factory2Id);
            Connection connection = new Connection(factory1, factory2, distance);
            factory1.addConnection(connection);
            factory2.addConnection(connection);
            this.connections.add(connection);
        }
    }

    public void update() {
        this.updateData();
        this.executeAction();
    }

    public void updateData() {
        this.troops = new ArrayList<Troop>();
        for (Connection connection : this.connections) {
            connection.resetTroops();
            connection.resetBombs();
        }
        int entityCount = in.nextInt();
        for (int i = 0; i < entityCount; i++) {
            int entityId = in.nextInt();
            String entityType = in.next();
            int arg1 = in.nextInt();
            int arg2 = in.nextInt();
            int arg3 = in.nextInt();
            int arg4 = in.nextInt();
            int arg5 = in.nextInt();
            if (entityType.equals("FACTORY")) {
                Factory factory = this.getFactory(entityId);
                Owner owner = Tools.convertNumberToOwner(arg1);
                factory.updateData(owner, arg2, arg3, arg4);
            } else if (entityType.equals("TROOP")) {
                Troop troop = this.getTroop(entityId);
                Factory start_factory = this.getFactory(arg2);
                Factory target_factory = this.getFactory(arg3);
                troop.updateData(arg1, start_factory, target_factory, arg4, arg5);
                Connection connection = this.getConnectionBetween(start_factory, target_factory);
                connection.addTroop(troop);
            } else if (entityType.equals("BOMB")) {
                Bomb bomb = this.getBomb(entityId);
                Owner owner = Tools.convertNumberToOwner(arg1);
                Factory start_factory = this.getFactory(arg2);
                Factory target_factory;
                int distance;
                if (arg3 == -1) {
                    if (!bomb.isMyProperty() && !bomb.isEnemyProperty()) { // Check if it is the first turn
                        target_factory = Bomb.getMostLikelyTarget(this.factories);
                        distance = start_factory.getDistanceTo(target_factory);
                    } else {
                        target_factory = bomb.getTargetFactory();
                        distance = bomb.getTimeUntilArrival() - 1;
                    }
                } else {
                    target_factory = this.getFactory(arg3);
                    distance = arg4;
                }
                bomb.updateData(owner, start_factory, target_factory, distance);
                Connection connection = this.getConnectionBetween(start_factory, target_factory);
                connection.addBomb(bomb);
            } else {
                throw new java.lang.Error("Entity type " + entityType + " unkown");
            }
        }
        this.main_factory = this.getMainFactory();
    }

    public Factory getMainFactory() {
        Factory best_option = this.getFactory(0);
        int best_defende_factories = -1;
        int best_distance_to_enemy = 2147483647;
        for (Factory factory : this.getMyFactories()) {
            ArrayList<Factory> factories = factory.getDefendedFactories();
            int defende_factories = factories.size();
            int distance_to_enemy = 0;
            if (factory.isConnectedWithEnemy()) {
                distance_to_enemy = factory.getDistanceTo(factory.getEnemyClosestFactory());
            }
            if (best_defende_factories <= defende_factories && best_distance_to_enemy > distance_to_enemy) {
                best_defende_factories = defende_factories;
                best_distance_to_enemy = distance_to_enemy;
                best_option = factory;
            }
        }
        return best_option;
    }

    public void executeAction() {
        String output_string = this.getMsg();
        ArrayList<GainAction> actions = this.getAllActions();

        // Active actions
        while (true) {
            actions.sort((o1, o2) -> Double.compare(o2.getScore(), o1.getScore()));
            GainAction action = actions.get(0);
            actions.remove(action);
            double score = action.getScore();
            if (score > 0) {
                output_string += ";" + action.executeAction();
            } else {
                System.err.println("Stopped with " + action.getClass().getSimpleName() + " score " + score
                        + " from factory " + action.start_factory.getId());
                break;
            }
        }

        // Bombs
        ArrayList<SendBomb> bombActions = new ArrayList<SendBomb>();
        for (Factory factory : this.getEnemyFactories()) {
            Factory start_factory = factory.getMyClosestFactory();
            SendBomb sendBomb = new SendBomb(start_factory, factory, this.main_factory);
            bombActions.add(sendBomb);
        }
        bombActions.sort((o1, o2) -> Double.compare(o2.getScore(), o1.getScore()));
        if (bombActions.size() > 0) {
            SendBomb action = bombActions.get(0);
            System.err.println("Bomb score is " + action.getScore());
            if (action.getScore() > 30) {
                output_string += ";" + action.executeAction();
            }
        }

        // Passive actions
        for (Factory factory : this.getMyFactories()) {
            if (factory.getId() != main_factory.getId()) {
                Defend defend = new Defend(factory, this.main_factory);
                String output = defend.executeAction();
                if (output != "") {
                    output_string += ";" + output;
                }
            }
        }

        System.out.println(output_string);
    }

    private ArrayList<GainAction> getAllActions() {
        ArrayList<GainAction> actions = new ArrayList<GainAction>();
        for (Factory factory : this.factories) {
            Upgrade upgrade = new Upgrade(factory, this.main_factory);
            actions.add(upgrade);
            if (factory.isMyProperty()) {
                for (Factory target_factory : this.factories) {
                    if (target_factory != factory) {
                        Attack attack = new Attack(factory, target_factory, this.main_factory);
                        actions.add(attack);
                        if (target_factory.isMyProperty() && factory.isMyProperty()) {
                            MoveAndUpgrade moveAndUpgrade = new MoveAndUpgrade(factory, target_factory,
                                    this.main_factory);
                            actions.add(moveAndUpgrade);
                        }
                    }
                }
            }
        }
        return actions;
    }

    private String getMsg() {
        String output_string = "MSG ";

        Factory main_factory = this.getMainFactory();

        int factory_id = main_factory.getId();
        Factory factory = this.getFactory(factory_id).simulateTurns(1);

        output_string += factory_id + ": " + factory.getAmountOfCyborgs() + " "
                + factory.isMyProperty();

        output_string += " Main base: " + main_factory.getId();
        return output_string;
    }
}

class Bomb {
    private int id;
    private Owner owner = Owner.NEUTRAL;
    private Factory start_factory;
    private Factory target_factory;
    private int distance;

    static Factory getMostLikelyTarget(ArrayList<Factory> factories) {
        int best_score = 0;
        Factory best_factory = factories.get(0);
        for (Factory factory : factories) {
            int score = factory.getAmountOfCyborgs() + factory.getProduction();
            if (factory.isMyProperty() && score > best_score) {
                best_score = score;
                best_factory = factory;
            }
        }
        return best_factory;
    }

    Bomb(int id) {
        this.id = id;
    }

    public void updateData(Owner owner, Factory start_factory, Factory target_factory, int distance) {
        this.owner = owner;
        this.start_factory = start_factory;
        this.target_factory = target_factory;
        this.distance = distance;
    }

    public int getId() {
        return this.id;
    }

    public Factory getTargetFactory() {
        return this.target_factory;
    }

    public int getTimeUntilArrival() {
        return this.distance;
    }

    public boolean isMyProperty() {
        return owner == Owner.PLAYER;
    }

    public boolean isEnemyProperty() {
        return owner == Owner.ENEMY;
    }
}

class Factory {
    private int id;
    private ArrayList<Connection> connections = new ArrayList<Connection>();
    private Owner owner = Owner.NEUTRAL;
    private int amount_of_cyborgs = 0;
    private int production = 0;
    private int turns_disabled = 0;

    Factory(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public void addConnection(Connection connection) {
        this.connections.add(connection);
    }

    public void updateData(Owner owner, int amount_of_cyborgs, int production, int turns_disabled) {
        this.owner = owner;
        this.amount_of_cyborgs = amount_of_cyborgs;
        this.production = production;
        this.turns_disabled = turns_disabled;
    }

    public int getTurnsDisabled() {
        return turns_disabled;
    }

    public void simulateActionMove(int amount) {
        this.amount_of_cyborgs -= amount;
    }

    public void simulateActionUpgrade() {
        this.amount_of_cyborgs -= 10;
        this.production += 1;
    }

    public boolean isMyProperty() {
        return this.owner == Owner.PLAYER;
    }

    public boolean isEnemyProperty() {
        return this.owner == Owner.ENEMY;
    }

    public boolean isNeutralProperty() {
        return this.owner == Owner.NEUTRAL;
    }

    public int getProduction() {
        return this.production;
    }

    public Factory copy() {
        Factory new_factory = new Factory(this.id);
        new_factory.updateData(this.owner, this.amount_of_cyborgs, this.production, this.turns_disabled);
        for (Connection connection : this.connections) {
            new_factory.addConnection(connection);
        }
        return new_factory;
    }

    public Factory simulateTurns(int turns) {
        Factory new_factory = this.copy();
        for (int turn = 1; turn <= turns; turn++) {
            new_factory.simulateTurn(turn);
        }
        return new_factory;
    }

    /**
     * Calculate the amount of cyborgs that you can take away without the factory
     * getting captured in x turns
     */
    public int getAmountOfExtraCyborgs(int turns) {
        Factory new_factory = this.copy();
        int amount_extra_cyborgs = new_factory.amount_of_cyborgs;
        for (int turn = 1; turn <= turns; turn++) {
            new_factory.simulateTurn(turn);
            if (new_factory.isMyProperty()) {
                if (new_factory.amount_of_cyborgs < amount_extra_cyborgs && new_factory.turns_disabled == 0) {
                    amount_extra_cyborgs = new_factory.amount_of_cyborgs;
                }
            }
        }
        return amount_extra_cyborgs;
    }

    public void simulateTurn(int turn) {
        this.turns_disabled -= Math.min(1, this.turns_disabled);
        this.simulateProduction();
        this.simulateAttackCyborgs(turn);
        this.simulateBombs(turn);
    }

    public void simulateProduction() {
        if ((this.isMyProperty() || this.isEnemyProperty()) && this.turns_disabled == 0) {
            this.amount_of_cyborgs += this.production;
        }
    }

    public void simulateAttackCyborgs(int turn) {
        ArrayList<Troop> troops = new ArrayList<Troop>();
        for (Connection connection : this.connections) {
            troops.addAll(connection.getArrivingTroops(this, turn));
        }
        int my_cyborgs = 0;
        int enemy_cyborgs = 0;
        for (Troop troop : troops) {
            if (troop.isMyTroop()) {
                my_cyborgs += troop.getAmountOfCybrogs();
            } else {
                enemy_cyborgs += troop.getAmountOfCybrogs();
            }
        }
        int attack = Math.min(my_cyborgs, enemy_cyborgs);
        my_cyborgs -= attack;
        enemy_cyborgs -= attack;
        if (this.isNeutralProperty()) {
            int defence = Math.min(Math.max(my_cyborgs, enemy_cyborgs), this.getAmountOfCyborgs());
            my_cyborgs -= defence;
            enemy_cyborgs -= defence;
            this.amount_of_cyborgs -= defence;
        } else if (this.isMyProperty()) {
            int defence = Math.min(enemy_cyborgs, this.getAmountOfCyborgs());
            enemy_cyborgs -= defence;
            this.amount_of_cyborgs -= defence;
        } else if (this.isEnemyProperty()) {
            int defence = Math.min(my_cyborgs, this.getAmountOfCyborgs());
            my_cyborgs -= defence;
            this.amount_of_cyborgs -= defence;
        }

        if (my_cyborgs > 0) {
            this.owner = Owner.PLAYER;
            this.amount_of_cyborgs = my_cyborgs;
        } else if (enemy_cyborgs > 0) {
            this.owner = Owner.ENEMY;
            this.amount_of_cyborgs = enemy_cyborgs;
        }
    }

    public void simulateBombs(int turn) {
        ArrayList<Bomb> bombs = new ArrayList<Bomb>();
        for (Connection connection : this.connections) {
            bombs.addAll(connection.getArrivingBombs(this, turn));
        }
        for (Bomb bomb : bombs) {
            if (this.amount_of_cyborgs < 10) {
                this.amount_of_cyborgs = 0;
            } else {
                this.amount_of_cyborgs -= (int) Math.floor((double) this.amount_of_cyborgs / 2);
            }
            this.turns_disabled = 5;
        }
    }

    public int getGainOnCapture(Owner Attacker) {
        if (this.owner == Attacker) {
            return 0;
        } else if (this.owner == Owner.NEUTRAL) {
            return this.production;
        } else {
            return 2 * this.production;
        }
    }

    public int getDistanceTo(Factory factory) {
        for (Connection connection : this.connections) {
            if (connection.getOtherSide(this) == factory) {
                return connection.getDistance();
            }
        }
        throw new java.lang.Error("Factory " + this.getId() + " is not connected to " + factory.getId());
    }

    /**
     * Get all factories that this factory can help when his nearest enemy base
     * attacks.
     **/
    public ArrayList<Factory> getDefendedFactories() {
        ArrayList<Factory> factories = new ArrayList<Factory>();
        for (Connection connection : this.connections) {
            Factory other_factory = connection.getOtherSide(this);
            if (other_factory.isMyProperty()) {
                if (other_factory.isConnectedWithEnemy()) {
                    Factory closestEnemyFactory = other_factory.getEnemyClosestFactory();
                    int distance = other_factory.getDistanceTo(closestEnemyFactory);
                    if (distance > connection.getDistance()) {
                        factories.add(other_factory);
                    }
                } else {
                    factories.add(other_factory);
                }
            }
        }
        return factories;
    }

    /**
     * Get all factories that are within a certain distance.
     **/
    public ArrayList<Factory> getMyFactoriesInsideDistance(int distance) {
        ArrayList<Factory> factories = new ArrayList<Factory>();
        for (Connection connection : this.connections) {
            Factory other_factory = connection.getOtherSide(this);
            if (other_factory.isMyProperty()) {
                int distance_to_factory = this.getDistanceTo(other_factory);
                if (distance_to_factory < distance) {
                    factories.add(other_factory);
                }
            }
        }
        return factories;
    }

    public int getAmountOfCyborgs() {
        return this.amount_of_cyborgs;
    }

    public boolean isConnectedWithEnemy() {
        for (Connection connection : this.connections) {
            Factory factory = connection.getOtherSide(this);
            if (factory.isEnemyProperty()) {
                return true;
            }
        }
        return false;
    }

    public Factory getEnemyClosestFactory() {
        this.connections.sort((o1, o2) -> Double.compare(o1.getDistance(), o2.getDistance()));
        for (Connection connection : this.connections) {
            Factory factory = connection.getOtherSide(this);
            if (factory.isEnemyProperty()) {
                return factory;
            }
        }
        throw new java.lang.Error("Factory " + this.getId() + " doesnt have a connection with enemy");
    }

    public Factory getMyClosestFactory() {
        this.connections.sort((o1, o2) -> Double.compare(o1.getDistance(), o2.getDistance()));
        for (Connection connection : this.connections) {
            Factory factory = connection.getOtherSide(this);
            if (factory.isMyProperty()) {
                return factory;
            }
        }
        throw new java.lang.Error("Factory " + this.getId() + " doesnt have a connection with enemy");
    }

    public Connection getConnectionWith(Factory factory) {
        for (Connection connection : this.connections) {
            if (connection.getOtherSide(this) == factory) {
                return connection;
            }
        }
        throw new java.lang.Error("Factory " + this.getId() + " is not connected with " + factory.getId());
    }
}

class Connection {
    private Factory[] factories;
    private int distance;
    private ArrayList<Troop> troops = new ArrayList<Troop>();
    private ArrayList<Bomb> bombs = new ArrayList<Bomb>();

    Connection(Factory factory_1, Factory factory_2, int distance) {
        this.factories = new Factory[] { factory_1, factory_2 };
        this.distance = distance;
    }

    public void resetTroops() {
        troops.clear();
    }

    public void addTroop(Troop troop) {
        troops.add(troop);
    }

    public void resetBombs() {
        bombs.clear();
    }

    public void addBomb(Bomb bomb) {
        bombs.add(bomb);
    }

    public ArrayList<Troop> getArrivingTroops(Factory target_factory, int turn) {
        ArrayList<Troop> troops = new ArrayList<Troop>();
        for (Troop troop : this.troops) {
            if (troop.getTargetFactory().getId() == target_factory.getId() && troop.getTimeUntilArrival() == turn) {
                troops.add(troop);
            }
        }
        return troops;
    }

    public ArrayList<Bomb> getArrivingBombs(Factory target_factory, int turn) {
        ArrayList<Bomb> bombs = new ArrayList<Bomb>();

        for (Bomb bomb : this.bombs) {
            if (bomb.getTargetFactory().getId() == target_factory.getId() && bomb.getTimeUntilArrival() == turn) {
                bombs.add(bomb);
            }
        }
        return bombs;
    }

    public Factory getOtherSide(Factory factory) {
        if (this.factories[0].getId() == factory.getId()) {
            return this.factories[1];
        } else if (this.factories[1].getId() == factory.getId()) {
            return this.factories[0];
        } else {
            throw new java.lang.Error("Factory " + factory.getId() + " is not in this connection");
        }
    }

    public int getDistance() {
        return this.distance;
    }

    public boolean connectsFactory(Factory factory) {
        if (this.factories[0] == factory || this.factories[1] == factory) {
            return true;
        } else {
            return false;
        }
    }

    public boolean connectsFactories(Factory factory_1, Factory factory_2) {
        if (this.connectsFactory(factory_1) && this.connectsFactory(factory_2)) {
            return true;
        } else {
            return false;
        }
    }

    public void showDebugInformation() {
        System.err.println("Connection from " + this.factories[0].getId() + " to " + this.factories[1].getId() + " ("
                + this.distance + ")");
    }
}

class Troop {
    private int id;
    private int owner;
    private Factory start_factory;
    private Factory target_factory;
    private int amount_of_cybrogs;
    private int time_until_arrival;

    Troop(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public void updateData(int owner, Factory start_factory, Factory target_factory, int amount_of_cybrogs,
            int time_until_arrival) {
        this.owner = owner;
        this.start_factory = start_factory;
        this.target_factory = target_factory;
        this.amount_of_cybrogs = amount_of_cybrogs;
        this.time_until_arrival = time_until_arrival;
    }

    public Factory getStartFactory() {
        return this.start_factory;
    }

    public Factory getTargetFactory() {
        return this.target_factory;
    }

    public int getAmountOfCybrogs() {
        return this.amount_of_cybrogs;
    }

    public int getTimeUntilArrival() {
        return this.time_until_arrival;
    }

    public boolean isMyTroop() {
        return this.owner == 1;
    }

    public boolean isEnemyTroop() {
        return this.owner == -1;
    }
}

abstract class Action {
    Factory main_factory;
    Factory start_factory;

    Action(Factory start_factory, Factory main_factory) {
        this.start_factory = start_factory;
        this.main_factory = main_factory;
    }

    public int getCyborgsAvaileble() {
        if (this.start_factory.getId() == this.main_factory.getId()) {
            // TODO calculate how many cyborgs are needed of defence
            return this.start_factory.getAmountOfCyborgs();
        }
        int distance = this.start_factory.getDistanceTo(this.main_factory);
        return this.start_factory.getAmountOfExtraCyborgs(distance + 1);
    }

    abstract String executeAction();
}

abstract class GainAction extends Action {

    GainAction(Factory start_factory, Factory main_factory) {
        super(start_factory, main_factory);
    }

    abstract int getTurnsNeeded();

    abstract int getProductionGain();

    abstract int getCyborgsNeeded();

    abstract int getCyborgsLoss();

    abstract boolean isPossible();

    public boolean isSmart() {
        if (this.getCyborgsNeeded() > this.getCyborgsAvaileble()) {
            return false;
        } else {
            return true;
        }
    }

    abstract double getSuccesChange();

    public double getScore() {
        if (this.isPossible() == false || this.isSmart() == false) {
            return 0.0;
        }
        int turns_needed = this.getTurnsNeeded();
        int production_gain = this.getProductionGain();
        int cyborgs_loss = this.getCyborgsLoss();
        double gain = (double) production_gain / cyborgs_loss - 0.01 * turns_needed;
        double succes_change = this.getSuccesChange();
        double score = succes_change * gain;
        return score;
    }
}

class Attack extends GainAction {
    private Factory target_factory;
    private Factory target_factory_future;

    Attack(Factory start_factory, Factory target_factory, Factory main_factory) {
        super(start_factory, main_factory);
        this.target_factory = target_factory;
        this.target_factory_future = this.target_factory.simulateTurns(this.getTurnsNeeded());
    }

    public Factory getStartFactory() {
        return this.start_factory;
    }

    public Factory getTargetFactory() {
        return this.target_factory;
    }

    public int getTurnsNeeded() {
        int distance = start_factory.getDistanceTo(this.target_factory);
        int turns_needed = distance + 1;
        return turns_needed;
    }

    public int getCyborgsNeeded() {
        int cyborgs_needed = this.target_factory_future.getAmountOfCyborgs() + 1;
        return cyborgs_needed;
    }

    public int getCyborgsLossEnemy() {
        int cyborgs_loss_enemy = (this.target_factory_future.isEnemyProperty()
                ? this.target_factory_future.getAmountOfCyborgs()
                : 0);
        return cyborgs_loss_enemy;
    }

    public int getCyborgsLoss() {
        int cyborgs_loss = this.getCyborgsNeeded() - this.getCyborgsLossEnemy();
        return cyborgs_loss;
    }

    public int getProductionGain() {
        int production_gain = this.target_factory_future.getGainOnCapture(Owner.PLAYER);
        return production_gain;
    }

    public boolean isPossible() {
        if (this.start_factory.getAmountOfCyborgs() < this.getCyborgsNeeded()
                || this.target_factory_future.isMyProperty()) {
            return false;
        } else {
            return true;
        }
    }

    public double getSuccesChange() {
        if (!target_factory.isConnectedWithEnemy()) {
            return 1.0;
        }
        Factory closest_enemy_factory = target_factory.getEnemyClosestFactory();
        int enemy_turns_needed = target_factory.getConnectionWith(closest_enemy_factory).getDistance() + 1;
        int my_turns_needed = this.getTurnsNeeded();
        if (my_turns_needed <= enemy_turns_needed) {
            return 1.0;
        } else {
            double change = Math.max(0.5 * Math.pow(3, (-0.5 * (my_turns_needed - enemy_turns_needed))), 0.0);
            return change;
        }
    }

    public String executeAction() {
        double score = Math.round(this.getScore() * 100.0) / 100.0;
        System.err.println(
                "Attack " + this.start_factory.getId() + "->" + this.target_factory.getId() + " Score: " + score);
        int cyborgs_needed = this.getCyborgsNeeded();
        String action = "MOVE " + this.start_factory.getId() + " " + this.target_factory.getId() + " " + cyborgs_needed;
        this.start_factory.simulateActionMove(cyborgs_needed);
        return action;
    }
}

class Upgrade extends GainAction {
    Upgrade(Factory start_factory, Factory main_factory) {
        super(start_factory, main_factory);
    }

    public int getProductionGain() {
        return 1;
    }

    public int getCyborgsNeeded() {
        return 10;
    }

    public int getCyborgsLoss() {
        return this.getCyborgsNeeded();
    }

    public boolean isPossible() {
        if (this.start_factory.isMyProperty() && this.start_factory.getAmountOfCyborgs() >= 10
                && this.start_factory.getProduction() < 3) {
            return true;
        } else {
            return false;
        }
    }

    public double getSuccesChange() {
        return 1.0;
    }

    public int getTurnsNeeded() {
        return 1;
    }

    public String executeAction() {
        double score = this.getScore();
        System.err.println("Upgrade " + this.start_factory.getId() + " Score: " + score);
        String action = "INC " + start_factory.getId();
        this.start_factory.simulateActionUpgrade();
        return action;
    }
}

class MoveAndUpgrade extends GainAction {
    private Factory target_factory;
    private Factory target_factory_future;

    MoveAndUpgrade(Factory start_factory, Factory target_factory, Factory main_factory) {
        super(start_factory, main_factory);
        this.target_factory = target_factory;
        this.target_factory_future = this.target_factory.simulateTurns(this.getTurnsNeeded());
    }

    public int getAmountOfUpgrades() {
        int upgrades = 1;// (int) Math.floor((double) this.getCyborgsAvaileble() / 10);
        upgrades = Math.min(upgrades, 3 - this.target_factory_future.getProduction());
        return upgrades;
    }

    public int getProductionGain() {
        return this.getAmountOfUpgrades();
    }

    public int getCyborgsNeeded() {
        return this.getAmountOfUpgrades() * 10;
    }

    public int getCyborgsLoss() {
        return this.getCyborgsNeeded();
    }

    public boolean isPossible() {
        if (this.target_factory_future.isMyProperty()
                && this.start_factory.getAmountOfCyborgs() >= this.getCyborgsNeeded()
                && this.target_factory_future.getProduction() < 3) {
            return true;
        } else {
            return false;
        }
    }

    public double getSuccesChange() {
        return 1.0;
    }

    public int getTurnsNeeded() {
        int distance = start_factory.getDistanceTo(this.target_factory);
        int turns_needed = distance + 2;
        return turns_needed;
    }

    public String executeAction() {
        double score = Math.round(this.getScore() * 100.0) / 100.0;
        System.err.println(
                "MoveAndUpgrade " + this.start_factory.getId() + "->" + this.target_factory.getId() + " with gain "
                        + this.getProductionGain() + " Score: " + score);
        String output = "MOVE " + this.start_factory.getId() + " " + this.target_factory.getId() + " "
                + this.getCyborgsNeeded();
        this.start_factory.simulateActionMove(this.getCyborgsNeeded());
        return output;
    }
}

class Defend extends Action {

    Defend(Factory start_factory, Factory main_factory) {
        super(start_factory, main_factory);
    }

    public String executeAction() {
        int cyborgs_needed = getCyborgsAvaileble();
        if (cyborgs_needed > 0) {
            System.err.println("Defend from " + this.start_factory.getId() + " to main factory");
            String action = "MOVE " + this.start_factory.getId() + " " + this.main_factory.getId() + " "
                    + cyborgs_needed;
            this.start_factory.simulateActionMove(cyborgs_needed);
            return action;
        } else {
            return "";
        }
    }
}

abstract class FightAction extends Action {
    Factory target_factory;

    FightAction(Factory start_factory, Factory target_factory, Factory main_factory) {
        super(start_factory, main_factory);
        this.target_factory = target_factory;
    }

    public double getScore() {
        if (!this.isPossible()) {
            return 0.0;
        }
        return this.enemyLoss() - this.playerLoss();
    }

    abstract boolean isPossible();

    abstract int enemyLoss();

    abstract int playerLoss();

    abstract String executeAction();
}

class SendBomb extends FightAction {

    SendBomb(Factory start_factory, Factory target_factory, Factory main_factory) {
        super(start_factory, target_factory, main_factory);
    }

    public int getTurnsNeeded() {
        int distance = this.start_factory.getDistanceTo(this.target_factory);
        return distance + 1;
    }

    public boolean isPossible() {
        if (this.target_factory.isEnemyProperty()) {
            return true;
        } else {
            return false;
        }
    }

    public int enemyLoss() {
        Factory factory = this.target_factory.simulateTurns(this.getTurnsNeeded());
        int direct_loss;
        if (factory.getAmountOfCyborgs() < 10) {
            direct_loss = factory.getAmountOfCyborgs();
        } else {
            direct_loss = (int) Math.floor((double) factory.getAmountOfCyborgs() / 2);
        }
        int indirect_loss = factory.getProduction() * (5 - factory.getTurnsDisabled());
        int loss = direct_loss + indirect_loss;
        return loss;
    }

    public int playerLoss() {
        return 0;
    }

    public String executeAction() {
        String action = "BOMB " + this.start_factory.getId() + " " + this.target_factory.getId();
        this.start_factory.simulateActionMove(this.playerLoss());
        return action;
    }
}

// Main
class Player {

    public static void main(String args[]) {
        Game game = new Game();
        game.start();
        while (true) {
            game.update();
        }
    }
}
