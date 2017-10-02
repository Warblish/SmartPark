package park.smartpark;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by staff on 10/1/2017.
 */

public class ParkingLot {
    public int pid;
    public LatLng position;
    public String lotname;
    public int totalspots;
    public int openspots;
    public int handicapspots;
    public int facultyspots;
    public boolean students;
    public LatLng getPosition() { return position; }
    public ParkingLot(int pid, LatLng position, String lotname, int totalspots, int openspots, int handicapspots, int facultyspots, boolean students) {
        this.pid = pid;
        this.position = position;
        this.lotname = lotname;
        this.totalspots = totalspots;
        this.openspots = openspots;
        this.handicapspots = handicapspots;
        this.facultyspots = facultyspots;
        this.students = students;
    }
}
