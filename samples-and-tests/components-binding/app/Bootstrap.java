import java.util.ArrayList;

import models.Actor;
import models.Address;
import models.Movie;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

@OnApplicationStart
public class Bootstrap extends Job {

	public void doJob() {
		if (Actor.count() == 0) {
			Movie pirate = new Movie();
			pirate.name = "Caribbean Pirates";
			pirate.save();

			Movie sleepy = new Movie();
			sleepy.name = "Sleepy Hollow";
			sleepy.save();

			Movie alice = new Movie();
			alice.name = "Alice in Wonderland";
			alice.save();

			Actor actor = new Actor();
			actor.name = "Johnny";

			actor.addresses = new ArrayList<Address>();
			Address principalHouse = new Address();
			principalHouse.streetName = "Sunset blvd";
			principalHouse.cityName = "Los Angeles";
			actor.addresses.add(principalHouse);

			Address secondaryHouse = new Address();
			secondaryHouse.streetName = "rte Grimaldi";
			secondaryHouse.cityName = "Monaco";
			actor.addresses.add(secondaryHouse);

			actor.movies = new ArrayList<Movie>();
			actor.movies.add(pirate);
			actor.movies.add(sleepy);

			actor.save();
		}
	}
}