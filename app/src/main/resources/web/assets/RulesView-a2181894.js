import{_ as ce}from"./FieldId-4280c3b8.js";import{_ as re}from"./Breadcrumb-99ff7c19.js";import{d as ae,C as ue,r as A,u as le,Z as J,af as R,bW as C,bX as pe,ac as me,K as _e,ad as H,o as c,U as fe,y as F,e,g as n,N as f,bY as T,c as r,F as y,A as w,j as d,ah as O,R as P,O as x,f as N,l as L,aj as ve,i as ge,t as he,a2 as j,bZ as be,b_ as $e,Q as ke,S as ee,T as te,w as oe,a1 as B}from"./index-cf22a9d8.js";import{T as b,a as $,_ as ye,A as we}from"./question-mark-rounded-956e83f8.js";import{_ as Ce}from"./DeleteConfirm.vuevuetypescriptsetuptruelang-1943d7a1.js";import{_ as Ve}from"./VModal.vuevuetypescriptsetuptruelang-83ac7574.js";import{u as Fe,a as Te}from"./vee-validate.esm-dd3b5249.js";import"./stringToArray-2bab501e.js";const Ae={class:"row"},Ne={class:"col-md-3 col-form-label"},De={class:"col-md-9 form-checks"},Ie={class:"form-check form-check-inline"},Ue={class:"form-check-label",for:"action-allow"},Me={class:"form-check form-check-inline"},Se={class:"form-check-label",for:"action-block"},Ee={class:"row mb-2"},qe={for:"action",class:"col-md-3 col-form-label"},Re={class:"col-md-9 form-checks"},Oe={class:"form-check form-check-inline"},Le={class:"form-check-label",for:"direction-inbound"},je={class:"form-check form-check-inline"},Be={class:"form-check-label",for:"direction-outbound"},Je={class:"row mb-3"},Qe={class:"col-md-3 col-form-label"},Ze={class:"col-md-9"},Ge=["value"],Ke={key:0,class:"input-group mt-2"},We=["placeholder"],Xe={class:"inner"},Ye={class:"help-block"},ze={value:""},He=["value"],Pe={key:2,class:"invalid-feedback"},xe={class:"row mb-3"},et={class:"col-md-3 col-form-label"},tt={class:"col-md-9"},ot={value:"all"},nt=["value"],at=["value"],lt={class:"row mb-3"},st={class:"col-md-3 col-form-label"},it={class:"col-md-9"},dt=["disabled"],ne=ae({__name:"EditRuleModal",props:{data:{type:Object},devices:{type:Array},networks:{type:Array}},setup(k){var Q,Z,G,K,W,X,Y;const v=k,{handleSubmit:h}=Fe(),s=ue({action:"block",direction:"inbound",protocol:"all",apply_to:"all",notes:"",target:"",is_enabled:!0}),u=A(b.DNS),D=Object.values(b),{t:I}=le(),{mutate:U,loading:M,onDone:S}=J({document:R`
    mutation createConfig($input: ConfigInput!) {
      createConfig(input: $input) {
        ...ConfigFragment
      }
    }
    ${C}
  `,options:{update:(a,i)=>{pe(a,i.data.createConfig,R`
          query {
            configs {
              ...ConfigFragment
            }
          }
          ${C}
        `)}}}),{mutate:E,loading:q,onDone:l}=J({document:R`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${C}
  `}),{value:p,resetField:m,errorMessage:g}=Te("inputValue",me().test("required",a=>"valid.required",a=>!$.hasInput(u.value)||!!a).test("target-value",a=>"invalid_value",a=>$.isValid(u.value,a??""))),t=(Q=v.data)==null?void 0:Q.data;s.action=(t==null?void 0:t.action)??"block",s.direction=(t==null?void 0:t.direction)??"inbound",s.protocol=(t==null?void 0:t.protocol)??"all",u.value=((G=(Z=v.data)==null?void 0:Z.target)==null?void 0:G.type)??b.DNS,p.value=((W=(K=v.data)==null?void 0:K.target)==null?void 0:W.value)??"",s.apply_to=((Y=(X=v.data)==null?void 0:X.applyTo)==null?void 0:Y.toValue())??"all",s.notes=(t==null?void 0:t.notes)??"",s.is_enabled=(t==null?void 0:t.is_enabled)??!0,t||m(),_e(u,(a,i)=>{(a===b.INTERFACE||i===b.INTERFACE)&&(p.value="")});const _=h(()=>{const a=new $;a.type=u.value,a.value=p.value??"",s.target=a.toValue(),v.data?E({id:v.data.id,input:{group:"rule",value:JSON.stringify(s)}}):U({input:{group:"rule",value:JSON.stringify(s)}})});return S(()=>{H()}),l(()=>{H()}),(a,i)=>{const se=ye,ie=ve,de=Ve;return c(),fe(de,{title:d(t)?a.$t("edit"):a.$t("create")},{body:F(()=>{var V,z;return[e("div",Ae,[e("label",Ne,n(a.$t("actions")),1),e("div",De,[e("div",Ie,[f(e("input",{class:"form-check-input",type:"radio",name:"action",id:"action-allow",value:"allow","onUpdate:modelValue":i[0]||(i[0]=o=>s.action=o)},null,512),[[T,s.action]]),e("label",Ue,n(a.$t("allow")),1)]),e("div",Me,[f(e("input",{class:"form-check-input",type:"radio",name:"action",id:"action-block",value:"block","onUpdate:modelValue":i[1]||(i[1]=o=>s.action=o)},null,512),[[T,s.action]]),e("label",Se,n(a.$t("block")),1)])])]),e("div",Ee,[e("label",qe,n(a.$t("direction")),1),e("div",Re,[e("div",Oe,[f(e("input",{class:"form-check-input",type:"radio",name:"direction",id:"direction-inbound",value:"inbound","onUpdate:modelValue":i[2]||(i[2]=o=>s.direction=o)},null,512),[[T,s.direction]]),e("label",Le,n(a.$t("inbound")),1)]),e("div",je,[f(e("input",{class:"form-check-input",type:"radio",name:"direction",id:"direction-outbound",value:"outbound","onUpdate:modelValue":i[3]||(i[3]=o=>s.direction=o)},null,512),[[T,s.direction]]),e("label",Be,n(a.$t("outbound")),1)])])]),e("div",Je,[e("label",Qe,n(a.$t("match")),1),e("div",Ze,[f(e("select",{class:"form-select","onUpdate:modelValue":i[4]||(i[4]=o=>u.value=o)},[(c(!0),r(y,null,w(d(D),o=>(c(),r("option",{value:o},n(a.$t(`target_type.${o}`)),9,Ge))),256))],512),[[O,u.value]]),d($).hasInput(u.value)?(c(),r("div",Ke,[f(e("input",{type:"text",class:"form-control","onUpdate:modelValue":i[5]||(i[5]=o=>P(p)?p.value=o:null),placeholder:a.$t("for_example")+" "+d($).hint(u.value)},null,8,We),[[x,d(p)]]),N(ie,{class:"input-group-text"},{content:F(()=>[e("pre",Ye,n(a.$t(`examples_${u.value}`)),1)]),default:F(()=>[e("span",Xe,[N(se,{class:"bi"})])]),_:1})])):L("",!0),u.value===d(b).INTERFACE?f((c(),r("select",{key:1,class:"form-select mt-2","onUpdate:modelValue":i[6]||(i[6]=o=>P(p)?p.value=o:null)},[e("option",ze,n(a.$t("all_local_networks")),1),(c(!0),r(y,null,w((V=k.networks)==null?void 0:V.filter(o=>o.type!=="wan"),o=>(c(),r("option",{value:o.ifName},n(o.name),9,He))),256))],512)),[[O,d(p)]]):L("",!0),d(g)?(c(),r("div",Pe,n(d(g)?a.$t(d(g)):""),1)):L("",!0)])]),e("div",xe,[e("label",et,n(d(I)("apply_to")),1),e("div",tt,[f(e("select",{class:"form-select","onUpdate:modelValue":i[7]||(i[7]=o=>s.apply_to=o)},[e("option",ot,n(a.$t("all_devices")),1),(c(!0),r(y,null,w((z=k.networks)==null?void 0:z.filter(o=>o.type!=="wan"),o=>(c(),r("option",{key:o.ifName,value:"iface:"+o.ifName},n(o.name),9,nt))),128)),(c(!0),r(y,null,w(k.devices,o=>(c(),r("option",{value:"mac:"+o.mac},n(o.name),9,at))),256))],512),[[O,s.apply_to]])])]),e("div",lt,[e("label",st,n(a.$t("notes")),1),e("div",it,[f(e("textarea",{class:"form-control","onUpdate:modelValue":i[8]||(i[8]=o=>s.notes=o),rows:"3"},null,512),[[x,s.notes]])])])]}),action:F(()=>[e("button",{type:"button",disabled:d(M)||d(q),class:"btn",onClick:i[9]||(i[9]=(...V)=>d(_)&&d(_)(...V))},n(a.$t("save")),9,dt)]),_:1},8,["title"])}}}),ct={class:"page-container container"},rt={class:"main"},ut={class:"v-toolbar"},pt={class:"table"},mt=e("th",null,"ID",-1),_t={class:"actions two"},ft={class:"form-check"},vt=["disabled","onChange","onUpdate:modelValue"],gt=["title"],ht=["title"],bt={class:"actions two"},$t=["onClick"],kt=["onClick"],Dt=ae({__name:"RulesView",setup(k){const v=A([]),h=A([]),s=A([]),{t:u}=le();ge({handle:(l,p)=>{p?he(u(p),"error"):(v.value=l.configs.filter(m=>m.group==="rule").map(m=>{const g=JSON.parse(m.value),t=new we;t.parse(g.apply_to);const _=new $;return _.parse(g.target),{id:m.id,createdAt:m.createdAt,updatedAt:m.updatedAt,data:g,applyTo:t,target:_}}),h.value=[...l.devices],s.value=[...l.networks])},document:j`
    query {
      configs {
        ...ConfigFragment
      }
      devices {
        ...DeviceFragment
      }
      networks {
        ...NetworkFragment
      }
    }
    ${be}
    ${C}
    ${$e}
  `});const D=j`
  mutation DeleteConfig($id: ID!) {
    deleteConfig(id: $id)
  }
`;function I(l){B(Ce,{id:l.id,name:l.id,gql:D,appApi:!1,typeName:"Config"})}function U(l){B(ne,{data:l,devices:h,networks:s})}function M(){B(ne,{data:null,devices:h,networks:s})}const{mutate:S,loading:E}=J({document:j`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${C}
  `});function q(l){S({id:l.id,input:{group:"rule",value:JSON.stringify(l.data)}})}return(l,p)=>{const m=re,g=ce;return c(),r("div",ct,[e("div",rt,[e("div",ut,[N(m,{current:()=>l.$t("page_title.rules")},null,8,["current"]),e("button",{type:"button",class:"btn right-actions",onClick:M},n(l.$t("create")),1)]),e("table",pt,[e("thead",null,[e("tr",null,[mt,e("th",null,n(l.$t("apply_to")),1),e("th",null,n(l.$t("description")),1),e("th",null,n(l.$t("notes")),1),e("th",null,n(l.$t("enabled")),1),e("th",null,n(l.$t("created_at")),1),e("th",null,n(l.$t("updated_at")),1),e("th",_t,n(l.$t("actions")),1)])]),e("tbody",null,[(c(!0),r(y,null,w(v.value,t=>(c(),r("tr",{key:t.id},[e("td",null,[N(g,{id:t.id,raw:t.data},null,8,["id","raw"])]),e("td",null,n(t.applyTo.getText(l.$t,h.value,s.value)),1),e("td",null,n(l.$t(`rule_${t.data.direction}`,{action:l.$t(t.data.action),target:t.target.getText(l.$t,s.value)})),1),e("td",null,n(t.data.notes),1),e("td",null,[e("div",ft,[f(e("input",{class:"form-check-input",disabled:d(E),onChange:_=>q(t),"onUpdate:modelValue":_=>t.data.is_enabled=_,type:"checkbox"},null,40,vt),[[ke,t.data.is_enabled]])])]),e("td",{class:"nowrap",title:d(ee)(t.createdAt)},n(d(te)(t.createdAt)),9,gt),e("td",{class:"nowrap",title:d(ee)(t.updatedAt)},n(d(te)(t.updatedAt)),9,ht),e("td",bt,[e("a",{href:"#",class:"v-link",onClick:oe(_=>U(t),["prevent"])},n(l.$t("edit")),9,$t),e("a",{href:"#",class:"v-link",onClick:oe(_=>I(t),["prevent"])},n(l.$t("delete")),9,kt)])]))),128))])])])])}}});export{Dt as default};
