from microbit import *

uart.init(baudrate=9600, bits=8, parity=None, stop=1)

led_on = Image("99999:"
             "99999:"
             "99999:"
             "99999:"
             "99999")
led_off = Image("00000:"
             "00000:"
             "00000:"
             "00000:"
             "00000")

time = 3000
old_direction = ''
old_gesture = ''
old_temperature = ''
old_light_level = ''
received_message_uart = ''
counter = time - 1
key = 69

def encrypt(key, content):
    encrypted_content = ''
    key = key % 90
    for char in content:
        ascii_code = ord(char) + key
        if ascii_code > 125:
            ascii_code = 36 + ascii_code % (125 + 1)
        encrypted_content += chr(ascii_code)
    return encrypted_content

def decrypt(key, content):
    decrypted_content = ''
    key = key % 90
    for char in content:
        ascii_code = ord(char) - key
        if ascii_code < 36:
            ascii_code = (125 + 1) - (36 - ascii_code)
        decrypted_content += chr(ascii_code)
    return decrypted_content

def receiveDataUart():
    global received_message_uart, key, time, counter
    received_message_uart = received_message_uart + str(uart.read(), 'UTF-8')
    if ('#' in received_message_uart) and ('!' in received_message_uart):
        start = received_message_uart.find('#') + 1
        end = received_message_uart.find('!')
        #display.scroll(decrypt(key, received_message_uart[1:-1]), 50)
        decrypted_message = decrypt(key, received_message_uart[start:end])
        splited_message = decrypted_message.split(':')
        if (splited_message[0] == 'l'): #this is the requirement
            if (splited_message[1] == '1'):
                display.show(led_on)
            elif (splited_message[1] == '0'):
                display.show(led_off)
        elif (splited_message[0] == 't'):
            time = int(splited_message[1], 10)
            counter = 0
        #radio.send(received_message_uart)
        received_message_uart = ''

def getDirection():
    degrees = compass.heading()
    direction = ''
    if (degrees <= 22.5) or (degrees >= 337.5):
        direction = 1 #'N'
    elif (degrees > 22.5) and (degrees < 67.5):
        direction = 2 #'NE'
    elif (degrees >= 67.5) and (degrees <= 112.5):
        direction = 3 #'E'
    elif (degrees > 112.5) and (degrees < 157.5):
        direction = 4 #'SE'
    elif (degrees >= 157.5) and (degrees <= 202.5):
        direction = 5 #'S'
    elif (degrees > 202.5) and (degrees < 247.5):
        direction = 6 #'SW'
    elif (degrees >= 247.5) and (degrees <= 292.5):
        direction = 7 #'W'
    elif (degrees > 292.5) and (degrees < 337.5):
        direction = 8 #'NW'
    return str(direction)

def getGesture():
    gesture = accelerometer.current_gesture()
    returnCode = '0'
    if (gesture == 'up'):
        returnCode = '1'
    elif (gesture == 'down'):
        returnCode = '2'
    elif (gesture == 'left'):
        returnCode = '3'
    elif (gesture == 'right'):
        returnCode = '4'
    elif (gesture == 'face up'):
        returnCode = '5'
    elif (gesture == 'face down'):
        returnCode = '6'
    elif (gesture == 'shake'):
        returnCode = '7'
    return returnCode

def getTemperature():
    return str(temperature())

def getLightLevel():
    return str(display.read_light_level())

def dictToJSON(inputDict):
    json = '{'
    for item in inputDict.items():
        json = json + item[0] + ':' + item[1] + ','
    json = json[:-1]
    json = json + '}'
    return json

compass.calibrate()

def process():
    data = dict()

    global old_gesture, old_direction, old_temperature, old_light_level, counter, key
    counter += 1

    new_gesture = getGesture()
    new_direction = getDirection()

    if (old_gesture != new_gesture):
        data['a'] = new_gesture
        data['l'] = old_light_level
        data['d'] = old_direction
        data['t'] = old_temperature
        old_gesture = new_gesture

    if (old_direction != new_direction):
        data['a'] = old_gesture
        data['l'] = old_light_level
        data['d'] = new_direction
        data['t'] = old_temperature
        old_direction = new_direction

    if (counter >= time):
        old_temperature = getTemperature()
        old_light_level = getLightLevel()
        data['a'] = old_gesture
        data['l'] = old_light_level
        data['d'] = old_direction
        data['t'] = old_temperature
        counter = 0

    if uart.any():
        receiveDataUart()

    if (len(data) == 4):
        message_encrypted = encrypt(key,dictToJSON(data))
        uart.write('#' + message_encrypted + '!')
        #uart.write(decrypt(key, message_encrypted) + '\n')
        #uart.write('====================================\n')

while True:
    process()
    sleep(10)