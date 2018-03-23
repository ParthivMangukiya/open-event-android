package org.fossasia.openevent.core.main;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.fossasia.openevent.common.ConstantStrings;
import org.fossasia.openevent.common.api.APIClient;
import org.fossasia.openevent.common.date.DateConverter;
import org.fossasia.openevent.data.Event;
import org.fossasia.openevent.data.Microlocation;
import org.fossasia.openevent.data.Session;
import org.fossasia.openevent.data.SessionType;
import org.fossasia.openevent.data.Speaker;
import org.fossasia.openevent.data.Sponsor;
import org.fossasia.openevent.data.Track;
import org.fossasia.openevent.data.extras.EventDates;
import org.fossasia.openevent.data.repository.RealmDataRepository;
import org.threeten.bp.format.DateTimeParseException;

import java.util.List;

import io.reactivex.Completable;
import io.realm.Realm;
import timber.log.Timber;

/**
 * Created by apple on 20/03/18.
 */

public class JsonHandler {

    public static Completable handleJsonEvent(String name, String json) {
        return Completable.fromAction(() -> {
            ObjectMapper objectMapper = APIClient.getObjectMapper();

            // Need separate instance for background thread
            Realm realm = Realm.getDefaultInstance();

            RealmDataRepository realmDataRepository = RealmDataRepository
                    .getInstance(realm);

            switch (name) {
                case ConstantStrings.EVENT: {
                    Event event = objectMapper.readValue(json, Event.class);

                    String startTime = event.getStartsAt();
                    String endTime = event.getEndsAt();
                    try {
                        List<EventDates> eventDates = DateConverter.getDaysInBetween(startTime, endTime);
                        realmDataRepository.saveEventDates(eventDates).subscribe();
                    } catch (DateTimeParseException dateTimeParseException) {
                        Timber.e(dateTimeParseException);
                        Timber.e("Error start parsing start date: %s and end date: %s in ISO format", startTime, endTime);
                        throw new Exception("Error parsing dates");
                    }

                    realmDataRepository.saveEvent(event).subscribe();
                    break;
                }
                case ConstantStrings.TRACKS: {
                    List<Track> tracks = objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, Track.class));
                    realmDataRepository.saveTracks(tracks).subscribe();
                    break;
                }
                case ConstantStrings.SESSIONS: {
                    List<Session> sessions = objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, Session.class));
                    for (Session current : sessions) {
                        current.setStartDate(current.getStartsAt().split("T")[0]);
                    }
                    realmDataRepository.saveSessions(sessions).subscribe();
                    break;
                }
                case ConstantStrings.SPEAKERS: {
                    List<Speaker> speakers = objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, Speaker.class));
                    realmDataRepository.saveSpeakers(speakers).subscribe();
                    break;
                }
                case ConstantStrings.SPONSORS: {
                    List<Sponsor> sponsors = objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, Sponsor.class));
                    realmDataRepository.saveSponsors(sponsors).subscribe();
                    break;
                }
                case ConstantStrings.MICROLOCATIONS: {
                    List<Microlocation> microlocations = objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, Microlocation.class));
                    realmDataRepository.saveLocations(microlocations).subscribe();
                    break;
                }
                case ConstantStrings.SESSION_TYPES: {
                    List<SessionType> sessionTypes = objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, SessionType.class));
                    realmDataRepository.saveSessionTypes(sessionTypes).subscribe();
                    break;
                }
                default:
                    //do nothing
            }
            realm.close();
        });
    }

}
