package net.sudologic.empires.states.gameplay;

import net.sudologic.empires.Game;
import net.sudologic.empires.states.gameplay.util.EmpireNameGenerator;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Empire {
    private String name;
    private ArrayList<Empire> allies, enemies;

    private ArrayList<Pixel> territory;
    private double[] ideology;

    private int maxSize = 0;
    private static float mergeDifficulty = 0.03f;
    private static float allianceDifficulty = 1.2f;

    private GameState gameState;

    private Color color;

    private Pixel capital;

    public Empire(GameState gameState) {
        ideology = new double[]{Math.random() * 255, Math.random() * 255, Math.random() * 255};
                                //CoopIso      AuthLib        LeftRight
        name = EmpireNameGenerator.generateEmpireName((int) ideology[0], (int) ideology[1], (int) ideology[2], null);
        color = new Color((int) (Math.random() * 255), (int) (Math.random() * 255), (int) (Math.random() * 255));
        territory = new ArrayList<>();
        enemies = new ArrayList<>();
        allies = new ArrayList<>();
        this.gameState = gameState;
    }

    public Empire(GameState gameState, String oldName) {
        ideology = new double[]{Math.random() * 255, Math.random() * 255, Math.random() * 255};
        //CoopIso      AuthLib        LeftRight
        String[] p = oldName.split(" ");
        this.name = EmpireNameGenerator.generateEmpireName((int) ideology[0], (int) ideology[1], (int) ideology[2], p[p.length - 1]);
        color = new Color((int) (Math.random() * 255), (int) (Math.random() * 255), (int) (Math.random() * 255));
        territory = new ArrayList<>();
        enemies = new ArrayList<>();
        allies = new ArrayList<>();
        this.gameState = gameState;
    }

    public void tick() {
        if (territory.size() == 0) {
            return;
        }
        if (enemies.size() == 0 && Math.random() < 0.1) {
            ideology[0] = ideology[0] * 0.9f;
        }
        if (capital.getEmpire() != this) {
            Pixel p = territory.get((int) (Math.random() * territory.size()));
            capital = p;
        }
        while (territory.contains(null)) {
            territory.remove(null);
        }
        if (maxSize > 0 && Math.random() > territory.size() / (maxSize * 0.66f) && territory.size() > 10) {
            Pixel p = territory.get((int) (Math.random() * territory.size()));
            if (p == null) {
                removeTerritory(null);
            } else {
                if (Math.random() < 0.1) {
                    setEnemy(p.revolt(), true);
                } else if (Math.random() < 0.1) {
                    for (Empire e : allies) {
                        if (ideoDifference(e) < (getCoopIso() + e.getCoopIso()) * (4 * Math.random()) * mergeDifficulty) {
                            mergeInto(e);
                        }
                    }
                }
            }

        }
        if (territory.size() > maxSize) {
            maxSize = territory.size();
        }
        for (Empire e : allies) {
            if (ideoDifference(e) < (getCoopIso() + e.getCoopIso()) * (4 * Math.random()) * mergeDifficulty) {
                mergeInto(e);
            }
        }
        ArrayList<Empire> deadEnemies = new ArrayList<>();
        for (Empire e : enemies) {
            if (!gameState.getEmpires().contains(e)) {
                deadEnemies.add(e);
            }
            if(allies.contains(e)) {
                allies.remove(e);
            }
            if(e.allies.contains(this)) {
                e.allies.remove(this);
            }
            if(!e.enemies.contains(this)) {
                e.enemies.add(this);
            }
        }
        for (Empire e : deadEnemies) {
            enemies.remove(e);
        }
    }

    public Pixel getCapital() {
        return capital;
    }

    public void setCapital(Pixel capital) {
        this.capital = capital;
    }

    public void removeTerritory(Pixel pixel) {
        if(territory.contains(pixel)) {
            if(pixel == null) {
                territory.remove(null);
                return;
            }
            if(pixel.getEmpire() == this) {
                pixel.setEmpire(null);
            }
            territory.remove(pixel);
        }
    }

    public void mergeInto(Empire e) {
        if(!gameState.getEmpires().contains(e) || e.getTerritory().size() == 0) {
            return;
        }
        System.out.println(name + " is merging into " + e.getName());
        for(Pixel p : territory) {
            if(p != null && p.getEmpire() == this) {
                p.setEmpire(e);
            }
        }
        territory.clear();
        gameState.removeEmpire(this);
    }

    public void render(Graphics g) {
        g.setColor(Color.white);
        int x = (int) (capital.getX() - (name.length() * 0.66f));
        int y = capital.getY();
        if(x < 0) {
            x = 0;
        }
        if(x + (name.length() / 0.8f) > gameState.getWidth()) {
            x -= (name.length() / 0.8f);
        }
        if(y < 2) {
            y = 2;
        }
        g.drawString(name, x * gameState.getScale(), y * gameState.getScale());
    }
    public double getCoopIso() {
        return ideology[0];
    }

    public double getAuthLib() {
        return ideology[1];
    }

    public double getLeftRight() {
        return ideology[2];
    }

    public void setEnemy(Empire e, boolean recur) {
        if(e == null) {
            return;
        }
        double coopIso = (float) ((ideology[0] + e.getCoopIso()) / 2);
        double ideoDiff = ideoDifference(e);
        if(allies.contains(e) && coopIso < ideoDiff * 0.16f * Math.random()) {
            allies.remove(e);
            e.allies.remove(this);
            setEnemy(e, false);
        }
        if(!enemies.contains(e)) {
            //System.out.println(name + " is now an enemy of " + e.getName());
            enemies.add(e);
            e.setEnemy(this, true);
            if(recur) {
                for(Empire a : allies) {
                    a.setEnemy(e, false);
                }
            }
        }
    }

    public void makePeace(Empire e) {
        if(enemies.contains(e)) {
            enemies.remove(e);
            e.enemies.remove(this);
            //System.out.println(name + " made peace with " + e.getName());
        }
    }

    public void setAlly(Empire e) {
        makePeace(e);
        if(allies.contains(e)) {
            return;
        }
        allies.add(e);
        e.allies.add(this);
        System.out.println(name + " is now allied with " + e.getName());
    }

    public double ideoDifference(Empire e) {
        double total = 0;
        for(int i = 0; i < ideology.length; i++) {
            total += Math.abs(ideology[i] - e.ideology[i]);
        }
        return total;
    }

    public void addTerritory(Pixel pixel) {
        //System.out.println(name + " gained territory!");
        if(pixel.getEmpire() != null) {
            pixel.getEmpire().removeTerritory(pixel);
        }
        if(!territory.contains(pixel)) {
            territory.add(pixel);
        }
        pixel.setEmpire(this);
    }

    public ArrayList<Pixel> getTerritory() {
        return territory;
    }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return color;
    }


    public ArrayList<Empire> getEnemies() {
        return enemies;
    }

    public Color getIdeologyColor() {
        return new Color((int) (ideology[0]), (int) (ideology[1]), (int) (ideology[2]));
    }

    public ArrayList<Empire> getAllies() {
        return allies;
    }

    public static float getAllianceDifficulty() {
        return allianceDifficulty;
    }
}
