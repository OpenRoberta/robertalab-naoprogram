from naoqi import *

class SpeechRecognitionModule(ALModule):

    def __init__(self, name):
        ALModule.__init__(self, name)
        self.asr = ALProxy("ALSpeechRecognition")
        self.memory = ALProxy("ALMemory")
        self.vocabulary = ["roberta"]
        self.lastWordRecognized = ""
        self.isWordRecognized = False
        self.asr.pause(1)
        self.asr.setVocabulary(self.vocabulary, True)
        self.asr.setAudioExpression(False)
        self.asr.setVisualExpression(False)
        self.asr.pause(0)
        self.BIND_PYTHON(self.getName(), "onWordRecognized")
        self.memory.subscribeToEvent("WordRecognized", self.getName(), "onWordRecognized")

    def setVocabulary(self, vocabulary):
        self.vocabulary = vocabulary
        self.asr.pause(1)
        self.asr.setVocabulary(self.vocabulary, True)
        self.asr.pause(0)

    def getVocabulary(self):
        return self.vocabulary

    def onWordRecognized(self, key, value, message):
        self.lastWordRecognized = self.memory.getData("LastWordRecognized")[0]
        self.isWordRecognized = True

    def pauseASR(self):
        self.asr.pause(1)

    def resumeASR(self):
        self.asr.pause(0)

    def toggleVisualExpression(self, state):
        self.asr.setVisualExpression(state)

    def toggleAudioExpression(self, state):
        self.asr.setAudioExpression(state)

    def recognizeWordFromDictionary(self, vocabulary):
        self.pauseASR()
        self.setVocabulary(vocabulary)
        self.resumeASR()
        while(!self.isWordRecognized):
            time.sleep(0.1)
        self.isWordRecognized = False
        return self.lastWordRecognized
