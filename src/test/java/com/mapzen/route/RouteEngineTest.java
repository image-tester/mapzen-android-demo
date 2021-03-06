package com.mapzen.route;

import com.mapzen.helpers.ZoomController;
import com.mapzen.osrm.Instruction;
import com.mapzen.osrm.Route;
import com.mapzen.support.MapzenTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import android.location.Location;

import static com.mapzen.support.TestHelper.MOCK_ACE_HOTEL;
import static com.mapzen.support.TestHelper.getTestLocation;
import static org.fest.assertions.api.Assertions.assertThat;

@Config(emulateSdk = 18)
@RunWith(MapzenTestRunner.class)
public class RouteEngineTest {
    private RouteEngine routeEngine;
    private Route route;
    private TestRouteListener listener;
    private ZoomController zoomController;

    @Before
    public void setUp() throws Exception {
        route = new Route(MOCK_ACE_HOTEL);
        listener = new TestRouteListener();
        zoomController = new ZoomController();

        routeEngine = new RouteEngine();
        routeEngine.setRoute(route);
        routeEngine.setListener(listener);
        routeEngine.setZoomController(zoomController);
    }

    @Test
    public void shouldNotBeNull() throws Exception {
        assertThat(routeEngine).isNotNull();
    }

    @Test
    public void onRecalculate_shouldNotifyWhenLost() throws Exception {
        Location location = getTestLocation(0, 0);
        routeEngine.onLocationChanged(location);
        assertThat(listener.recalculating).isTrue();
    }

    @Test
    public void onSnapLocation_shouldReturnCorrectedLocation() throws Exception {
        Location location = getTestLocation(40.7444114, -73.9904202);
        routeEngine.onLocationChanged(location);
        assertThat(listener.originalLocation).isEqualsToByComparingFields(location);
        assertThat(listener.snapLocation).isEqualsToByComparingFields(route.snapToRoute(location));
    }

    @Test
    public void onEnterInstructionRadius_shouldReturnIndex() throws Exception {
        route.addSeenInstruction(route.getRouteInstructions().get(0));
        routeEngine.onLocationChanged(route.getRouteInstructions().get(1).getLocation());
        assertThat(listener.enterIndex).isEqualTo(1);
    }

    @Test
    public void onExitInstructionRadius_shouldReturnIndex() throws Exception {
        routeEngine.onLocationChanged(route.getRouteInstructions().get(0).getLocation());
        routeEngine.onLocationChanged(route.getRouteInstructions().get(1).getLocation());
        assertThat(listener.exitIndex).isEqualTo(0);
    }

    @Test
    public void onUpdateDistance_shouldReturnDistanceToNextInstruction() throws Exception {
        routeEngine.onLocationChanged(route.getRouteInstructions().get(0).getLocation());
        assertThat(listener.closestDistance).isEqualTo(0);
    }

    @Test
    public void onUpdateDistance_shouldReturnFullRouteDistanceAtStart() throws Exception {
        routeEngine.onLocationChanged(route.getRouteInstructions().get(0).getLocation());
        assertThat(listener.distanceToDestination).isEqualTo(route.getTotalDistance());
    }

    @Test
    public void onUpdateDistance_shouldCountdownDistanceToDestinationAtTurn() throws Exception {
        routeEngine.onLocationChanged(route.getRouteInstructions().get(0).getLocation());
        routeEngine.onLocationChanged(route.getRouteInstructions().get(1).getLocation());
        assertThat(listener.distanceToDestination).isEqualTo(route.getTotalDistance() -
                route.getRouteInstructions().get(0).getDistance());
    }

    @Test
    public void onUpdateDistance_shouldReturnZeroAtDestination() throws Exception {
        for (Instruction instruction : route.getRouteInstructions()) {
            routeEngine.onLocationChanged(instruction.getLocation());
        }

        assertThat(listener.distanceToDestination).isEqualTo(0);
        assertThat(listener.instructionDistance).isEqualTo(0);
    }

    @Test
    public void onUpdateDistance_shouldReturnInstructionDistance() throws Exception {
        routeEngine.onLocationChanged(route.getRouteInstructions().get(0).getLocation());
        assertThat(listener.instructionDistance)
                .isEqualTo(route.getRouteInstructions().get(0).getDistance());
    }

    @Test
    public void onUpdateDistance_shouldCountdownInstructionDistance() throws Exception {
        Location location = getTestLocation(40.743810, -73.989053); // 26th & Broadway
        routeEngine.onLocationChanged(route.getRouteInstructions().get(0).getLocation());
        routeEngine.onLocationChanged(location);
        assertThat(listener.instructionDistance).isEqualTo((int) route.getRouteInstructions()
                .get(0).getRemainingDistance(route.snapToRoute(location)));
    }

    @Test
    public void onUpdateDistance_shouldCountdownDistanceToDestinationAlongRoute() throws Exception {
        Instruction instruction = route.getRouteInstructions().get(0);
        Location location = getTestLocation(40.743810, -73.989053); // 26th & Broadway
        routeEngine.onLocationChanged(instruction.getLocation());
        routeEngine.onLocationChanged(location);
        int expected = route.getTotalDistance() - instruction.getDistance()
                + instruction.getRemainingDistance(route.snapToRoute(location));
        assertThat(listener.distanceToDestination).isEqualTo(expected);
    }

    private static class TestRouteListener implements RouteEngine.RouteListener {
        private Location originalLocation;
        private Location snapLocation;

        private boolean recalculating = false;
        private int enterIndex = -1;
        private int exitIndex = -1;
        private int closestDistance = -1;
        private int instructionDistance = -1;
        private int distanceToDestination = -1;

        @Override
        public void onRecalculate(Location location) {
            recalculating = true;
        }

        @Override
        public void onSnapLocation(Location originalLocation, Location snapLocation) {
            this.originalLocation = originalLocation;
            this.snapLocation = snapLocation;
        }

        @Override
        public void onEnterInstructionRadius(int index) {
            enterIndex = index;
        }

        @Override
        public void onExitInstructionRadius(int index) {
            exitIndex = index;
        }

        @Override
        public void onUpdateDistance(int closestDistance, int instructionDistance,
                int distanceToDestination) {
            this.closestDistance = closestDistance;
            this.instructionDistance = instructionDistance;
            this.distanceToDestination = distanceToDestination;
        }
    }
}
