package org.vadere.simulator.util;

import org.apache.commons.math3.util.Pair;
import org.junit.Before;
import org.junit.Test;
import org.vadere.state.attributes.scenario.AttributesObstacle;
import org.vadere.state.attributes.scenario.builder.AttributesObstacleBuilder;
import org.vadere.state.attributes.scenario.builder.AttributesSourceBuilder;
import org.vadere.state.attributes.scenario.builder.AttributesTargetBuilder;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.*;

import static org.junit.Assert.assertEquals;

public class TopographyCheckerTest {

	TopographyTestBuilder builder;

	@Before
	public void setup() {
		builder = new TopographyTestBuilder();
	}

	@Test
	public void testCheckObstacleOverlapHasOverlap() {
		Topography topography = new Topography();

		Obstacle obs1 = new Obstacle(new AttributesObstacle(0, new VRectangle(0, 0, 1, 1)));
		Obstacle obs2 = new Obstacle(new AttributesObstacle(1, new VRectangle(0, 0, 1, 1)));

		topography.addObstacle(obs1);
		topography.addObstacle(obs2);

		TopographyChecker topcheck = new TopographyChecker(topography);

		List<Pair<Obstacle, Obstacle>> actualList = topcheck.checkObstacleOverlap();

		assertEquals(1, actualList.size());
	}


	@Test
	public void testCheckObstacleOverlapHasNoOverlap() {
		Topography topography = new Topography();

		Obstacle obs1 = new Obstacle(new AttributesObstacle(0, new VRectangle(0, 0, 1, 1)));
		Obstacle obs2 = new Obstacle(new AttributesObstacle(1, new VRectangle(1.1, 0, 1, 1)));
		topography.addObstacle(obs1);
		topography.addObstacle(obs2);

		TopographyChecker topcheck = new TopographyChecker(topography);

		List<Pair<Obstacle, Obstacle>> actualList = topcheck.checkObstacleOverlap();

		assertEquals(0, actualList.size());
	}

	@Test
	public void testCheckObstacleOverlapReturnsNoOverlapsIfTwoSegmentsTouch() {
		Topography topography = new Topography();

		Obstacle obs1 = new Obstacle(new AttributesObstacle(0, new VRectangle(0, 0, 1, 1)));
		Obstacle obs2 = new Obstacle(new AttributesObstacle(1, new VRectangle(1, 0, 1, 1)));
		topography.addObstacle(obs1);
		topography.addObstacle(obs2);

		TopographyChecker topcheck = new TopographyChecker(topography);

		List<Pair<Obstacle, Obstacle>> actualList = topcheck.checkObstacleOverlap();

		assertEquals(0, actualList.size());
	}

	// Test checkUniqueSourceId

	/**
	 * There should be non unique ids
	 */
	@Test
	public void testCheckUniqueSourceIdNegative() {
		builder.addSource(); //id = -1 ok first
		builder.addSource(); //id = -1 err
		builder.addSource(); //id = -1 err
		builder.addSource(2); // ok first
		builder.addSource(2); // err
		Topography topography = builder.build();

		TopographyChecker checker = new TopographyChecker(topography);
		List<TopographyCheckerMessage> out = checker.checkUniqueSourceId();

		assertEquals("The sources should have the same id", 3, out.size());
		out.forEach(m -> assertEquals(TopographyCheckerReason.SOURCE_ID_NOT_UNIQUE, m.getReason()));
	}

	/**
	 * There should be only unique ids
	 */
	@Test
	public void testCheckUniqueSourceIdPositive() {
		builder.addSource(1);
		builder.addSource(2);
		builder.addSource(3);
		Topography topography = builder.build();

		TopographyChecker checker = new TopographyChecker(topography);
		List<TopographyCheckerMessage> out = checker.checkUniqueSourceId();

		assertEquals("No warnings expected", 0, out.size());
	}

	// Test checkValidTargetsInSource

	@Test
	public void TestCheckValidTargetsInSourceNoIdNoSpawn() {
		AttributesSourceBuilder attrBuilder = AttributesSourceBuilder.anAttributesSource();
		builder.addSource(attrBuilder
				.spawnNumber(0)
				.targetIds(new ArrayList<>())
				.build()
		);
		Topography topography = builder.build();

		TopographyChecker checker = new TopographyChecker(topography);
		List<TopographyCheckerMessage> out = checker.checkValidTargetsInSource();

		assertEquals(1, out.size());
		assertEquals(TopographyCheckerReason.SOURCE_NO_TARGET_ID_NO_SPAWN, out.get(0).getReason());
	}

	@Test
	public void TestCheckValidTargetsInSourceNoId() {
		AttributesSourceBuilder attrBuilder = AttributesSourceBuilder.anAttributesSource();
		builder.addSource(attrBuilder
				.targetIds(new ArrayList<>())
				.build()
		);
		Topography topography = builder.build();

		TopographyChecker checker = new TopographyChecker(topography);
		List<TopographyCheckerMessage> out = checker.checkValidTargetsInSource();

		assertEquals(1, out.size());
		assertEquals(TopographyCheckerReason.SOURCE_NO_TARGET_ID_SET, out.get(0).getReason());
		assertEquals(TopographyCheckerMessageType.ERROR, out.get(0).getMsgType());
	}


	@Test
	public void TestCheckValidTargetsInSourceWrongId() {
		AttributesSourceBuilder attrSourceB = AttributesSourceBuilder.anAttributesSource();
		AttributesTargetBuilder attrTargetB = AttributesTargetBuilder.anAttributesTarget();
		builder.addSource(attrSourceB
				.targetIds(Collections.singletonList(4)) // id not found !
				.build()
		);
		builder.addTarget(attrTargetB
				.id(1)
				.build());

		Topography topography = builder.build();

		TopographyChecker checker = new TopographyChecker(topography);
		List<TopographyCheckerMessage> out = checker.checkValidTargetsInSource();

		assertEquals(1, out.size());
		assertEquals(TopographyCheckerReason.SOURCE_TARGET_ID_NOT_FOUND, out.get(0).getReason());
		assertEquals(TopographyCheckerMessageType.ERROR, out.get(0).getMsgType());
	}

	@Test
	public void TestCheckValidTargetsInSourceWithSomeWrongId() {
		AttributesSourceBuilder attrSourceB = AttributesSourceBuilder.anAttributesSource();
		AttributesTargetBuilder attrTargetB = AttributesTargetBuilder.anAttributesTarget();
		builder.addSource(attrSourceB
				.targetIds(Arrays.asList(1, 2, 3)) // id 3 not found !
				.build()
		);
		builder.addTarget(attrTargetB
				.id(1)
				.build());
		builder.addTarget(attrTargetB
				.id(3)
				.build());


		Topography topography = builder.build();

		TopographyChecker checker = new TopographyChecker(topography);
		List<TopographyCheckerMessage> out = checker.checkValidTargetsInSource();

		assertEquals(1, out.size());
		assertEquals(TopographyCheckerReason.SOURCE_TARGET_ID_NOT_FOUND, out.get(0).getReason());
		assertEquals(TopographyCheckerMessageType.ERROR, out.get(0).getMsgType());
		assertEquals("[2]", out.get(0).getReasonModifier());
	}

	@Test
	public void TestCheckValidTargetsInSourceNoError() {
		AttributesSourceBuilder attrSourceB = AttributesSourceBuilder.anAttributesSource();
		AttributesTargetBuilder attrTargetB = AttributesTargetBuilder.anAttributesTarget();
		builder.addSource(attrSourceB
				.targetIds(Collections.singletonList(1))
				.build()
		);
		builder.addTarget(attrTargetB
				.id(1)
				.build());

		Topography topography = builder.build();

		TopographyChecker checker = new TopographyChecker(topography);
		List<TopographyCheckerMessage> out = checker.checkValidTargetsInSource();

		assertEquals(0, out.size());
	}

	// Test checkSourceObstacleOverlap

	@Test
	public void testCheckSourceObstacleOverlapWithNoOverlap(){
		AttributesObstacleBuilder attrObstacleB = AttributesObstacleBuilder.anAttributesObstacle();
		AttributesSourceBuilder attrSourceB = AttributesSourceBuilder.anAttributesSource();

		builder.addSource(attrSourceB
				.shape(new VRectangle(0,0,10,10))
				.build());

		builder.addObstacle(attrObstacleB
				.shape(new VRectangle(15,15,5,5))
				.build());

		Topography topography = builder.build();
		TopographyChecker checker = new TopographyChecker(topography);

		List<TopographyCheckerMessage> out = checker.checkSourceObstacleOverlap();

		assertEquals( 0, out.size());
	}


	@Test
	public void testCheckSourceObstacleOverlapWithOverlap(){
		AttributesObstacleBuilder attrObstacleB = AttributesObstacleBuilder.anAttributesObstacle();
		AttributesSourceBuilder attrSourceB = AttributesSourceBuilder.anAttributesSource();

		builder.addSource(attrSourceB
				.shape(new VRectangle(0,0,10,10))
				.build());
		Source testSource = (Source)builder.getLastAddedElement();

		builder.addSource(attrSourceB
				.shape(new VRectangle(100,100,10,10))
				.build());


		builder.addObstacle(attrObstacleB
				.shape(new VCircle(0,0,5.0))
				.build());
		Obstacle testObstacle = (Obstacle) builder.getLastAddedElement();

		builder.addObstacle(attrObstacleB
				.shape(new VRectangle(15,15,5,5))
				.build());

		Topography topography = builder.build();
		TopographyChecker checker = new TopographyChecker(topography);

		List<TopographyCheckerMessage> out = checker.checkSourceObstacleOverlap();

		TopographyCheckerMessage msg = out.get(0);
		assertEquals( 1, out.size());
		assertEquals(TopographyCheckerMessageType.ERROR, msg.getMsgType());
		assertEquals(TopographyCheckerReason.SOURCE_OVERLAP_WITH_OBSTACLE, msg.getReason());
		assertEquals(testSource, msg.getMsgTarget().getTargets().get(0));
		assertEquals(testObstacle, msg.getMsgTarget().getTargets().get(1));

	}


}