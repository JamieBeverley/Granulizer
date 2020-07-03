Granulizer : Object {

	var <> deviceName;
	var <> deviceIndex;
	var <> knobs;
	var <> knobOffset;
	var <> notes;
	var <> synth;
	var <> gainIndex;
	var <> posIndex;
	var <> rateIndex;
	var <> durIndex;
	var <> panIndex;
	var <> bufferOffset;
	var <> buffers;
	var <> buffer;
	var <> synth;
	classvar <> knobMap;


	*initClass{
		Granulizer.knobMap = ['gain','pos','posJitter','rate','dur','pan','panJitter','reverb'];
	}

	init{
		Server.default.waitForBoot({
			this.deviceName = "MIDIEndPoint(\"Launchkey Mini\", \"Launchkey Mini MIDI 1\")";
			this.deviceIndex;
			this.knobOffset = 21;
			this.notes = [60];
			this.synth;

			this.gainIndex = 0;
			this.posIndex = 1;
			this.rateIndex = 2;
			this.durIndex = 3;
			this.panIndex = 4;
			this.bufferOffset = 36;
			this.buffers=0!8;
			this.buffer =0;
			// fml
			this.buffers[0] = Buffer.readChannel(Server.default, "/home/jamie/.local/share/SuperCollider/downloaded-quarks/Dirt-Samples/choir/1.wav", channels:[0]);

			this.buffers[1] = Buffer.readChannel(Server.default, "/home/jamie/.local/share/SuperCollider/downloaded-quarks/Dirt-Samples/soulvoc/Soul Vocal 100BPM C  01.wav", channels:[0]);

			this.buffers[2] = Buffer.readChannel(Server.default, "/home/jamie/.local/share/SuperCollider/downloaded-quarks/Dirt-Samples/soulvoc/Soul Vocal 100BPM C  02.wav", channels:[0]);

			this.buffers[3] = Buffer.readChannel(Server.default, "/home/jamie/.local/share/SuperCollider/downloaded-quarks/Dirt-Samples/soulvoc/Soul Vocal 100BPM C  03.wav", channels:[0]);

			this.buffers[4] = Buffer.readChannel(Server.default, "/home/jamie/.local/share/SuperCollider/downloaded-quarks/Dirt-Samples/soulvoc/Soul Vocal 100BPM C  04.wav", channels:[0]);

			this.buffers[5] = Buffer.readChannel(Server.default, "/home/jamie/.local/share/SuperCollider/downloaded-quarks/Dirt-Samples/soulvoc/Soul Vocal 100BPM C  05.wav", channels:[0]);

			this.buffers[6] = Buffer.readChannel(Server.default, "/home/jamie/.local/share/SuperCollider/downloaded-quarks/Dirt-Samples/soulvoc/Soul Vocal 100BPM C  06.wav", channels:[0]);

			this.buffers[7] = Buffer.readChannel(Server.default, "/home/jamie/.local/share/SuperCollider/downloaded-quarks/Dirt-Samples/soulvoc/Soul Vocal 100BPM C  07.wav", channels:[0]);
			"Sample buffers loaded.".postln;
			this.synthDef();
			"SynthDef loaded.".postln;
		});
	}

	*new{
		^super.new.init();
	}

	midi {
		var f;
		MIDIClient.init();
		MIDIClient.sources.do{
			|i,index|
			if(i.asString==this.deviceName,{this.deviceIndex=index});
		};

		f = {
			MIDIIn.connect(inport:this.deviceIndex,device:MIDIClient.sources.at(this.deviceIndex));
			MIDIdef(\granulizer_cc,{
				|val,nm,chan,src|
				var knob = Granulizer.knobMap[nm-this.knobOffset];
				var value = val/127;
				[knob, value, nm, chan, src].postln;
				this.synth.set(knob, value);
			},msgType:\control).permanent_(true);

			MIDIdef(\granulizer_note_noteOn,{
				|val,nm,chan,src|
				[val,nm,chan,src].postln;
				this.synth.set(\freq, nm.midicps);
			},msgType:\noteOn,chan:8).permanent_(true);

			MIDIdef(\granulizer_note_noteOff,{
				|val,nm,chan,src|
				[val,nm,chan,src].postln;
			},msgType:\noteOff, chan:8).permanent_(true);

			MIDIdef(\granulizer_buffer_choose_noteOn,{
				|val,nm,chan,src|
				[val,nm,chan,src].postln;
				this.buffer=nm-this.bufferOffset;
				this.synth.set('bufnum', this.buffers[this.buffer]);
			},msgType:\noteOn, chan:9).permanent_(true);
			"Granulizer MidiDefs added.".postln;
		};
		f.value();
	}

	start {
		this.synth = Synth.new(\granulizer,[bufnum:this.buffers[this.buffer]]);
	}

	synthDef {
		SynthDef(\granulizer,{
			|gain=0.2,pos=0, posJitter=0,rate=1,dur=0.5, pan=0.5, panJitter=0, reverb=0, bufnum, freq=261|
			var audio = TGrains.ar(
				2,
				trigger:Impulse.kr(rate*rate*32),
				bufnum:bufnum,
				rate:(freq.cpsmidi-60).midiratio,
				centerPos:((pos)+WhiteNoise.kr(mul:posJitter*posJitter*posJitter).range(-1,1))*BufDur.kr(bufnum),
				dur: dur*dur*2,
				pan: ((2*pan)-1) + ((WhiteNoise.kr(mul:panJitter).range(0,1)*2)-1)
			)*gain;
			// var rv = FreeVerb.ar(audio,mix:0,room:0.9,mul:reverb)+audio;
			Out.ar(0, audio);
		}).add;
	}

	play {
		this.synth = {
			var audio = TGrains.ar(1, trigger:Impulse.kr(this.knobs[this.durIndex]), bufnum:this.buffers[this.buffer], rate:(this.note-60).midiratio,centerPos:0,dur:this.knobs[this.durIndex], pan:this.knobs[this.panIndex]);
			Out.ar(0, audio);
		};
		this.synth.play;
		^this.synth;
	}

}