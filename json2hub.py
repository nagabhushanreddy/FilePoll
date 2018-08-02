import traceback
import json
msgDef = {}


def setMessageDefinition():
    global msgDef
    msgDef = {'objDelim': ':', 'attrDelim': '#', 'valueDelim': '=', 'start': '{', 'end': '}'}


def getHubMsg():
    ##write code to read from database
    return '{TRADE:{TRDEEXT:splitno=0#service=MOTR#extref=123ABC#}{TRDEEXT:splitno=0#service=MOBE#extref=1#}' \
           '{TRDPARTY:splitno=0#partyref=PQR456CD#partyreftype=INT#ptytyp=NIP NOM#ptyacc= #ptypchg= #}' \
           '{TRDPARTY:partyreftype=COMP#splitno=0#partyref=HBHAI135J#ptytyp=NIP NOM#ptypchg= #ptyacc= #}' \
           '{TRDECODE:splitno=0#trdcode=Q#trdclass=1786#}{TRDECODE:splitno=0#trdcode=I#trdclass=1886#} ' \
           'priority=40#buysell=BUY#type=PRIN#pqty=10000#}';

def getHubMsg2():
    ##write code to read from database2
    return '{TRADE:{TRDEEXT:splitno=0#service=MOTR#extref=123ABC#}{TRDEEXT:splitno=0#service=MOBE#extref=2#}' \
           '{TRDPARTY:splitno=0#partyref=PQR456CD#partyreftype=INT#ptytyp=NIP NOM#ptyacc= #ptypchg= #}' \
           '{TRDPARTY:partyreftype=COMP#splitno=0#partyref=HBHAI135J#ptytyp=NIP NOM#ptypchg= #ptyacc= #}' \
           '{TRDECODE:splitno=0#trdcode=Q#trdclass=1786#}{TRDECODE:splitno=0#trdcode=I#trdclass=1886#} ' \
           'priority=40#buysell=BUY#type=PRIN#pqty=10000#}';

def makeJSON(hubMsg, msgDef):
    result = ""
    for x in hubMsg:
        key = ''
        for k in msgDef:
            if msgDef[k] == x:
                key = k
        if (result.endswith(msgDef["start"]) and key != "start"):
            result = result + "\""
        if (result.endswith(msgDef["start"]) and key == "start"):
            result = result[:-1]+"["
        if (result.endswith(msgDef["end"]+",") and key == "start"):
            result = result[:-1]+"}"+","
        if (key == "objDelim"):
            result = result + "\"" + ":"+"{"
        if (key == "start"):
            result = result + "{"
        if (key == "end"):
            result = result[:-2] + "},"
        if (key == "attrDelim"):
            result = result + "\""+ ","+"\""
        if (key == "valueDelim"):
            result = result + "\""+":" +"\""
        if (result.rstrip().endswith('},')  and x.isalnum()):
            result = result.rstrip()[:-1] + "}" +"]" +","+"\""
        if(key==''):
            result=result+x
    result=result.rstrip(",")
    return json.loads(result)

def checkTradeEquals(trade1, trade2):
    for tref1 in trade1["TRADE"]:
        if "TRDEEXT" in tref1:
            if(tref1["TRDEEXT"]["service"]=="MOTR"):
                for tref2 in trade2["TRADE"]:
                    if "TRDEEXT" in tref2:
                        if(tref2["TRDEEXT"]["service"]=="MOTR"):
                            return tref1["TRDEEXT"]["extref"] == tref2["TRDEEXT"]["extref"]
    return False

def main(args):
    try:
        global msgDef
        setMessageDefinition()
        msg1=makeJSON(getHubMsg(), msgDef)
        msg2=makeJSON(getHubMsg2(), msgDef)

        if(checkTradeEquals(msg1, msg2)):
            print("Both trades are equal");
            print msg1
            print msg2

    except Exception as e:
        #traceback.print_exc()
        print("Error!")
    finally:
        print("Cleanup and exit.")


if __name__ == '__main__':
    import sys

    sys.exit(main(sys.argv))
