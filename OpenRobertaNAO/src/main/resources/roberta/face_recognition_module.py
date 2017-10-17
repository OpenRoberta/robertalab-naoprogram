from naoqi import *
import time

class FaceRecognitionModule(ALModule):

    def __init__(self, name):
        ALModule.__init__(self, name)
        self.fd = ALProxy("ALFaceDetection")
        self.memory = ALProxy("ALMemory")
        self.BIND_PYTHON(self.getName(), "onFaceRecognized")
        self.memory.subscribeToEvent("FaceDetected", self.getName(), "onFaceRecognized")
        self.fd.setRecognitionEnabled(False)
        self.lastFaceRecognized = ""
        self.isFaceRecognized = False
        self.fd.clearDatabase()
        self.naoFaceInformation = []

    def learnFace(self, name):
        return self.fd.learnFace(name)

    def forgetFace(self, name):
        return self.fd.forgetPerson(name)

    def detectFace(self):
        self.fd.setRecognitionEnabled(True)
        while(not self.isFaceRecognized):
            time.sleep(0.1)
        self.isFaceRecognized = False
        self.fd.setRecognitionEnabled(False)
        return self.lastFaceRecognized

    def getNaoFaceInformation(self):
        return self.naoFaceInformation


    def onFaceRecognized(self, key, value, message):
        if len(self.memory.getData("FaceDetected")) == 0:
            self.lastFaceRecognized = ""
            self.isFaceRecognized = False
        else:
            self.lastFaceRecognized = self.memory.getData("FaceDetected")[1][0][1][2]
            self.isFaceRecognized = True
            # self.naoFaceInformation = mark[0][1:] 

    def unsubscribe(self):
        self.memory.unsubscribeToEvent("FaceDetected", self.getName())
