(
var buffers = [
	Buffer.readChannel(Server.default, Platform.userAppSupportDir++"/Extensions/Granulizer/samples/1.wav", channels:[0]),
	Buffer.readChannel(Server.default, Platform.userAppSupportDir++"/Extensions/Granulizer/samples/2.wav", channels:[0]),
	Buffer.readChannel(Server.default, Platform.userAppSupportDir++"/Extensions/Granulizer/samples/3.wav", channels:[0]),
	Buffer.readChannel(Server.default, Platform.userAppSupportDir++"/Extensions/Granulizer/samples/4.wav", channels:[0]),
	Buffer.readChannel(Server.default, Platform.userAppSupportDir++"/Extensions/Granulizer/samples/5.wav", channels:[0]),
	Buffer.readChannel(Server.default, Platform.userAppSupportDir++"/Extensions/Granulizer/samples/6.wav", channels:[0]),
	Buffer.readChannel(Server.default, Platform.userAppSupportDir++"/Extensions/Granulizer/samples/7.wav", channels:[0]),
	Buffer.readChannel(Server.default, Platform.userAppSupportDir++"/Extensions/Granulizer/samples/8.wav", channels:[0])
];
buffers.postln;

MIDISynthDefFX(
	name:\granulizer,
	ugenGraphFunc:{
		|out=0, gate=1, amp=0.1, attack=0.01, release=1, pos=0, posJitter=0.13,rate=1,dur=0.5, pan=0.5, panJitter=0, reverb=0, bufnum=0, freq=261|
		var audio = TGrains.ar(
			2,
			trigger:Impulse.kr(rate*rate*32),
			bufnum:bufnum,
			rate:(freq.cpsmidi-60).midiratio,
			centerPos:((pos)+WhiteNoise.kr(mul:posJitter*posJitter*posJitter).range(-1,1))*BufDur.kr(bufnum),
			dur: dur*dur*2,
			pan: ((2*pan)-1) + ((WhiteNoise.kr(mul:panJitter).range(0,1)*2)-1)
		)*amp;
		var env = EnvGen.ar(Env.asr(attack,1,release), gate:gate, doneAction:2);
		audio = audio*env;
		Out.ar(out, audio);
	},
	fxGraphFunc:{
		|in, reverb=0.2|
		var z,y, dry, audio;
		dry = In.ar(in, 2);
		audio = dry;

		// Reverb
		z = DelayN.ar(audio, 0.048);
		y = Mix.ar(Array.fill(7,{ CombL.ar(z, 0.1, LFNoise1.kr(0.1.rand, 0.04, 0.05), 15) }));
		4.do({ y = AllpassN.ar(y, 0.050, [0.050.rand, 0.050.rand], 1) });
		audio = audio+(reverb*y);
		audio;
	},
	ccMap:(
		21: [\amp, {|x| x/127}],
		22: [\attack, {|x| x*2/127 }],
		23: [\release, {|x| x*4/127 }],
		24: [\pos, {|x| (x/127) }],
		25: [\posJitter, {|x| (x/127)}],
		26: [\dur, {|x| x/127/2}],
		27: [\bufnum, {|x| buffers[(x*7/127).floor].bufnum}],
	),
	fxCCMap:(
		28: [\reverb, {|x| (x/127).pow(2)*0.3}],
	),
	permanent:true,
	polyphony:8,
	verbose:true
).add(midiChan: 6);
)
