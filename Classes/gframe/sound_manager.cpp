#include "sound_manager.h"
#include "config.h"
#include "game.h"
#include "data_manager.h"

namespace ygo {

bool SoundManager::Init(double sounds_volume, double music_volume, bool sounds_enabled, bool music_enabled, void* payload) {
	soundsEnabled = sounds_enabled;
	musicEnabled = music_enabled;
	rnd.seed(time(0));
	bgm_scene = -1;
	bgm_process = true;
	RefreshBGMList();
	RefreshChantsList();
    try {
        openal = Utils::make_unique<YGOpen::OpenALSingleton>();
        sfx = Utils::make_unique<YGOpen::OpenALSoundLayer>(openal);
        bgm = Utils::make_unique<YGOpen::OpenALSoundLayer>(openal);
        sfx->setVolume(sounds_volume);
        bgm->setVolume(music_volume);
        return true;
    }
    catch (std::runtime_error& e) {
        return soundsEnabled = musicEnabled = false;
    }
}
void SoundManager::RefreshBGMList() {
	Utils::Makedirectory(TEXT("./sound/BGM/"));
	Utils::Makedirectory(TEXT("./sound/BGM/duel"));
	Utils::Makedirectory(TEXT("./sound/BGM/menu"));
	Utils::Makedirectory(TEXT("./sound/BGM/deck"));
	Utils::Makedirectory(TEXT("./sound/BGM/advantage"));
	Utils::Makedirectory(TEXT("./sound/BGM/disadvantage"));
	Utils::Makedirectory(TEXT("./sound/BGM/win"));
	Utils::Makedirectory(TEXT("./sound/BGM/lose"));
	Utils::Makedirectory(TEXT("./sound/BGM/custom"));
	Utils::Makedirectory(TEXT("./sound/chants"));
	RefreshBGMDir(TEXT(""), BGM::DUEL);
	RefreshBGMDir(TEXT("duel"), BGM::DUEL);
	RefreshBGMDir(TEXT("menu"), BGM::MENU);
	RefreshBGMDir(TEXT("deck"), BGM::DECK);
	RefreshBGMDir(TEXT("advantage"), BGM::ADVANTAGE);
	RefreshBGMDir(TEXT("disadvantage"), BGM::DISADVANTAGE);
	RefreshBGMDir(TEXT("win"), BGM::WIN);
	RefreshBGMDir(TEXT("lose"), BGM::LOSE);
	RefreshBGMDir(TEXT("custom"), BGM::CUSTOM);
}
void SoundManager::RefreshBGMDir(path_string path, BGM scene) {
	for(auto& file : Utils::FindfolderFiles(TEXT("./sound/BGM/") + path, { TEXT("mp3"), TEXT("ogg"), TEXT("wav") })) {
		auto conv = Utils::ToUTF8IfNeeded(path + TEXT("/") + file);
		BGMList[BGM::ALL].push_back(conv);
		BGMList[scene].push_back(conv);
	}
}
void SoundManager::RefreshChantsList() {
	for(auto& file : Utils::FindfolderFiles(TEXT("./sound/chants"), { TEXT("mp3"), TEXT("ogg"), TEXT("wav") })) {
		auto scode = Utils::GetFileName(TEXT("./sound/chants/") + file);
		try {
			unsigned int code = std::stoi(scode);
			if (code && !ChantsList.count(code))
				ChantsList[code] = Utils::ToUTF8IfNeeded(file);
		}
		catch (std::exception& e) {
			Utils::Deletefile(Utils::ToUTF8IfNeeded(TEXT("./sound/chants/") + file));
		}
	}
}
void SoundManager::PlaySoundEffect(SFX sound) {
    static const std::map<SFX, const char*> fx = {
        {SUMMON, "./sound/summon.wav"},
        {SPECIAL_SUMMON, "./sound/specialsummon.wav"},
        {ACTIVATE, "./sound/activate.wav"},
        {SET, "./sound/set.wav"},
        {FLIP, "./sound/flip.wav"},
        {REVEAL, "./sound/reveal.wav"},
        {EQUIP, "./sound/equip.wav"},
        {DESTROYED, "./sound/destroyed.wav"},
        {BANISHED, "./sound/banished.wav"},
        {TOKEN, "./sound/token.wav"},
        {NEGATE, "./sound/negate.wav"},
        {ATTACK, "./sound/attack.wav"},
        {DIRECT_ATTACK, "./sound/directattack.wav"},
        {DRAW, "./sound/draw.wav"},
        {SHUFFLE, "./sound/shuffle.wav"},
        {DAMAGE, "./sound/damage.wav"},
        {RECOVER, "./sound/gainlp.wav"},
        {COUNTER_ADD, "./sound/addcounter.wav"},
        {COUNTER_REMOVE, "./sound/removecounter.wav"},
        {COIN, "./sound/coinflip.wav"},
        {DICE, "./sound/diceroll.wav"},
        {NEXT_TURN, "./sound/nextturn.wav"},
        {PHASE, "./sound/phase.wav"},
        {SOUND_MENU, "./sound/menu.wav"},
        {BUTTON, "./sound/button.wav"},
        {INFO, "./sound/info.wav"},
        {QUESTION, "./sound/question.wav"},
        {CARD_PICK, "./sound/cardpick.wav"},
        {CARD_DROP, "./sound/carddrop.wav"},
        {PLAYER_ENTER, "./sound/playerenter.wav"},
        {NEGATE, "./sound/negate.wav"},
        {CHAT, "./sound/chatmessage.wav"}
    };
    if (!soundsEnabled) return;
    if (sfx) sfx->play(fx.at(sound), false);
}
void SoundManager::PlayDialogSound(irr::gui::IGUIElement * element) {
	if(element == mainGame->wMessage) {
		PlaySoundEffect(INFO);
	} else if(element == mainGame->wQuery) {
		PlaySoundEffect(QUESTION);
	} else if(element == mainGame->wSurrender) {
		PlaySoundEffect(QUESTION);
	} else if(element == mainGame->wOptions) {
		PlaySoundEffect(QUESTION);
	} else if(element == mainGame->wANAttribute) {
		PlaySoundEffect(QUESTION);
	} else if(element == mainGame->wANCard) {
		PlaySoundEffect(QUESTION);
	} else if(element == mainGame->wANNumber) {
		PlaySoundEffect(QUESTION);
	} else if(element == mainGame->wANRace) {
		PlaySoundEffect(QUESTION);
	} else if(element == mainGame->wReplaySave) {
		PlaySoundEffect(QUESTION);
	} else if(element == mainGame->wFTSelect) {
		PlaySoundEffect(QUESTION);
	}
}
void SoundManager::PlayMusic(const std::string& song, bool loop) {
	if(!musicEnabled) return;
    StopBGM();
    if (bgm) bgmCurrent = bgm->play(song, loop);
    bgm_process = true;
}
void SoundManager::PlayBGM(BGM scene) {
	if(!mainGame->chkMusicMode->isChecked())
		scene = BGM::ALL;
	auto& list = BGMList[scene];
	int count = list.size();
	if (musicEnabled && (scene != bgm_scene || !bgm->exists(bgmCurrent)) && bgm_process && count > 0) {
		bgm_scene = scene;
		int bgm = (std::uniform_int_distribution<>(0, count - 1))(rnd);
		bgm_PlayingName = list[bgm];
		std::string BGMName = "./sound/BGM/" + list[bgm];
		PlayMusic(BGMName, false);
	}
}
void SoundManager::StopSound() {
	sfx->stopAll();
}
void SoundManager::StopBGM() {
    bgm->stopAll();
}
bool SoundManager::PlayChant(unsigned int code) {
    CardData cd;
    if(dataManager.GetData(code, &cd) && (cd.alias != 0))
        code = cd.alias;

	if(ChantsList.count(code) && bgm_PlayingName != ChantsList[code]) {
		if (bgm) {
			bgm_process = false;
			bgm_PlayingName = ChantsList[code];
			PlayMusic("./sound/chants/" + ChantsList[code], false);
		}
		return true;
	}
	return false;
}
void SoundManager::PlayCustomSound(char* SoundName) {
	if (!soundsEnabled) return;
	if (access(SoundName, 0) != 0) return;
	if (sfx) sfx->play(SoundName, false);
}
void SoundManager::PlayCustomBGM(char* BGMName) {
	if (!musicEnabled || !mainGame->chkMusicMode->isChecked()) return;
	if (access(BGMName, 0) != 0) return;

	std::string CustomBGM = Utils::GetFileName(TEXT(BGMName));
	if (bgm && bgm_PlayingName != CustomBGM) {
		bgm_process = false;
		bgm_PlayingName = CustomBGM;
		PlayMusic(BGMName, false);
	}
}
void SoundManager::SetSoundVolume(double volume) {
    if (sfx) sfx->setVolume(volume);
}
void SoundManager::SetMusicVolume(double volume) {
    if (bgm) bgm->setVolume(volume);
}
void SoundManager::EnableSounds(bool enable) {
	soundsEnabled = enable;
	if(!soundsEnabled) {
		StopSound();
	}
}
void SoundManager::EnableMusic(bool enable) {
	musicEnabled = enable;
	if(!musicEnabled) {
        StopBGM();
    }
}

} // namespace ygo
