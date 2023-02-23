package main

import (
	"context"
	"encoding/json"
	"github.com/rsocket/rsocket-go/extension"
	"log"

	cloudevents "github.com/cloudevents/sdk-go/v2"
	"github.com/rsocket/rsocket-go"
	"github.com/rsocket/rsocket-go/payload"
)

func main() {
	var c = make(chan bool)

	// Connect to server
	cli, err := rsocket.Connect().
		SetupPayload(payload.Empty()).
		MetadataMimeType(extension.MessageCompositeMetadata.String()).
		DataMimeType(cloudevents.ApplicationCloudEventsJSON).
		Acceptor(func(ctx context.Context, socket rsocket.RSocket) rsocket.RSocket {
			//responder
			return nil
		}).
		Transport(rsocket.TCPClient().SetHostAndPort("127.0.0.1", 9527).Build()).
		Start(context.Background())
	if err != nil {
		panic(err)
	}
	defer cli.Close()

	// Send request
	bytes, _ := json.Marshal(event())

	rb, _ := extension.EncodeRouting("cloudevents")

	cmb := extension.CompositeMetadataBuilder{}
	cmb.PushWellKnown(extension.MessageRouting, rb)
	cmb.PushWellKnown(extension.CloudEventsJSON, bytes)

	cm, _ := cmb.Build()

	result, err := cli.RequestResponse(payload.New(bytes, cm)).
		Block(context.Background())
	if err != nil {
		panic(err)
	}

	res := event()
	_ = json.Unmarshal(result.Data(), &res)
	log.Println("response:", res)

	<-c
}

func event() cloudevents.Event {
	event := cloudevents.NewEvent()
	event.SetSource("example/uri")
	event.SetType("example.type")
	_ = event.SetData(cloudevents.ApplicationJSON, map[string]string{"hello": "world"})
	return event
}