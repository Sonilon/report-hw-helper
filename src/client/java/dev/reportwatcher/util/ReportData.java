package dev.reportwatcher.util;

public class ReportData {
    public int slot;
    public String complainantName = "";
    public int complainantPJ = 0;
    public int complainantLJ = 0;
    public int complainantPP = 0;
    public int complainantLP = 0;
    public String suspectName = "";
    public boolean suspectHasDonate = false;
    public int suspectPP = 0;
    public int suspectLP = 0;
    public int suspectPJ = 0;
    public int suspectLJ = 0;
    public String server = "";
    public String createdDate = "";
    public String lastCheckDate = "";
    public int weight = 0;
    public boolean isFree = false;

    public boolean matchesTrigger() {
        boolean complainantGood = complainantPJ > complainantLJ;
        boolean suspectNoPriv = !suspectHasDonate && suspectPP == 0;
        return complainantGood && suspectNoPriv;
    }
}
