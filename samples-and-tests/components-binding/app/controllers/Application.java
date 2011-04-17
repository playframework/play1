package controllers;

import java.util.Collection;
import java.util.List;

import models.Actor;
import models.Address;
import models.Movie;
import play.mvc.Controller;

public class Application extends Controller {

	public static void index(Long actorId) {
		List<Movie> allMovies = Movie.findAll();
		Actor actor = Actor.find("order by id desc").first();
		render(actor, allMovies);
	}

	public static void save(Actor actor) {
		actor.save();// That's All!
		index(actor.id); //redirect to show actor's page
	}

	public static void removeAddress(Long actorId, Long addressId) {
		Actor actor = Actor.findById(actorId);
		actor.addresses.remove(Address.findById( addressId));
		actor.save();
		index(actor.id);
	}

	public static void massSaveAddresses(Long actorId, Collection<Address> justAddresses) {
		for (Address address : justAddresses) {
	        address.save();
        }
		index(actorId);
	}

}