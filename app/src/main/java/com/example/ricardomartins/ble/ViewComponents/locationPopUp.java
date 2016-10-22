package com.example.ricardomartins.ble.ViewComponents;

import android.util.Log;
import android.util.StringBuilderPrinter;
import android.view.Menu;
import android.widget.PopupMenu;

/**
 * Created by ricardomartins on 26/09/16.
 */
public class locationPopUp {

    private StringBuilder room = new StringBuilder(" ");
    private StringBuilder floor = new StringBuilder(" ");
    private StringBuilder building = new StringBuilder(" ");

    private StringBuilder street = new StringBuilder(" ");
    private StringBuilder number = new StringBuilder(" ");
    private StringBuilder zipcode = new StringBuilder(" ");
    private StringBuilder city = new StringBuilder(" ");
    private StringBuilder country = new StringBuilder(" ");


    private PopupMenu menu;
    private Menu pop_menu;


    public locationPopUp(PopupMenu menu){
        this.menu = menu;
        pop_menu = menu.getMenu();

        pop_menu.add(Menu.NONE,1,1,"Room: " );
        pop_menu.add(Menu.NONE,2,2,"Floor: ");
        pop_menu.add(Menu.NONE,3,3,"Building: " );
        pop_menu.add(Menu.NONE,4,4,"Street: ");
        pop_menu.add(Menu.NONE,5,5,"Number: ");
        pop_menu.add(Menu.NONE,6,6,"Zipcode: ");
        pop_menu.add(Menu.NONE,7,7,"City: ");
        pop_menu.add(Menu.NONE,8,8,"Country: ");

    }

    public void update(String room, String floor, String building,String street, String number, String zipcode,String city, String country  ){

        Log.i("ok", this.room.toString() + "  " + room);
        if( this.room.toString() != room){
            this.room.replace(0, room.length(),room);
            pop_menu.removeItem(1);
            pop_menu.add(Menu.NONE,1,1,"Room: " +  room);
        }
        if ( this.floor.toString() != floor){
            this.floor.replace(0, floor.length(),floor);
            pop_menu.removeItem(2);
            menu.getMenu().add(Menu.NONE,2,2,"Floor: " +  floor);
        }
        if ( this.building.toString() != building){
            this.building.replace(0,building.length(),building);
            pop_menu.removeItem(3);
            menu.getMenu().add(Menu.NONE,3,3,"Building: " +  building);
        }
        if ( this.street.toString() != street){
            this.street.replace(0,street.length(),street);
            pop_menu.removeItem(4);
            menu.getMenu().add(Menu.NONE,4,4,"Street: " +  street);
        }
        if ( this.number.toString() != number){
            this.number.replace(0, number.length(), number);
            pop_menu.removeItem(5);
            menu.getMenu().add(Menu.NONE,5,5,"Number: " +  number);
        }
        if ( this.zipcode.toString() != zipcode){
            this.zipcode.replace(0,zipcode.length(),zipcode);
            pop_menu.removeItem(6);
            menu.getMenu().add(Menu.NONE,6,6,"Zipcode: " +  zipcode);
        }
        if ( this.city.toString() != city){
            this.city.replace(0,city.length(),city);
            pop_menu.removeItem(7);
            menu.getMenu().add(Menu.NONE,7,7,"City: " +  city);
        }
        if ( this.country.toString() != country){
            this.country.replace(0,country.length(),country);
            pop_menu.removeItem(8);
            menu.getMenu().add(Menu.NONE,8,8,"Country: " +  country);
        }

    }

    public void show(){
        menu.show();
    }
}



